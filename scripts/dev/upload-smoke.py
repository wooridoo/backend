#!/usr/bin/env python
from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import uuid
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

try:
    import jwt  # type: ignore
    import requests  # type: ignore
    from PIL import Image, ImageDraw  # type: ignore
except ImportError as exc:  # pragma: no cover
    print(
        "[upload-smoke] Missing dependency:",
        exc,
        file=sys.stderr,
    )
    print(
        "[upload-smoke] Install with: python -m pip install requests pyjwt pillow",
        file=sys.stderr,
    )
    sys.exit(2)


@dataclass
class ResultRow:
    case_id: str
    expected: str
    actual: str
    status_code: int
    api_message: str
    passed: bool
    evidence: str


def parse_env_value(env_path: Path, key: str) -> str:
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith(f"{key}="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError(f"Missing {key} in {env_path}")


def make_token(user_id: str, secret: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": user_id,
        "email": f"{user_id}@qa.local",
        "type": "access",
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(hours=2)).timestamp()),
    }
    return jwt.encode(payload, secret, algorithm="HS256")


def make_jpeg(path: Path, width: int, height: int, label: str) -> None:
    image = Image.new("RGB", (width, height), (54, 104, 190))
    draw = ImageDraw.Draw(image)
    draw.text((20, 20), label, fill=(255, 255, 255))
    image.save(path, "JPEG", quality=88)


def make_large_file(path: Path, size_bytes: int) -> None:
    path.write_bytes(os.urandom(size_bytes))


def call_multipart(
    session: requests.Session,
    method: str,
    url: str,
    token: str,
    files: list[tuple[str, tuple[str, bytes, str]]],
) -> tuple[int, dict[str, Any], str]:
    response = session.request(
        method=method,
        url=url,
        headers={"Authorization": f"Bearer {token}"},
        files=files,
        timeout=60,
    )
    raw = response.text
    try:
        body = response.json()
    except Exception:
        body = {}
    return response.status_code, body, raw


def write_outputs(output_dir: Path, rows: list[ResultRow], metadata: dict[str, Any]) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    results_json = output_dir / "results.json"
    summary_json = output_dir / "summary.json"
    results_csv = output_dir / "results.csv"

    results_json.write_text(
        json.dumps([asdict(row) for row in rows], ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    summary = {
        "timestampUtc": datetime.now(timezone.utc).isoformat(),
        "resultCount": len(rows),
        "passCount": sum(1 for row in rows if row.passed),
        "failCount": sum(1 for row in rows if not row.passed),
        **metadata,
    }
    summary_json.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    with results_csv.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["case_id", "passed", "status_code", "api_message", "expected", "actual", "evidence"])
        for row in rows:
            writer.writerow(
                [
                    row.case_id,
                    row.passed,
                    row.status_code,
                    row.api_message,
                    row.expected,
                    row.actual,
                    row.evidence,
                ]
            )


def main() -> int:
    parser = argparse.ArgumentParser(description="Upload policy smoke test")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--env-file", default="backend/.env.local.properties")
    parser.add_argument("--out-dir", default="")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parents[3]
    env_path = (repo_root / args.env_file).resolve()
    if not env_path.exists():
        print(f"[upload-smoke] env file not found: {env_path}", file=sys.stderr)
        return 2

    run_ts = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
    out_dir = Path(args.out_dir) if args.out_dir else (repo_root / ".tmp" / f"verify-{run_ts}")
    assets_dir = out_dir / "assets"
    evidence_dir = out_dir / "evidence"
    assets_dir.mkdir(parents=True, exist_ok=True)
    evidence_dir.mkdir(parents=True, exist_ok=True)

    secret = parse_env_value(env_path, "JWT_SECRET")
    session = requests.Session()

    challenge_resp = session.get(f"{args.base_url}/challenges", params={"page": 1, "size": 1}, timeout=30)
    if challenge_resp.status_code != 200:
        print("[upload-smoke] failed to bootstrap challenge list", file=sys.stderr)
        return 2
    challenge_json = challenge_resp.json()
    content = challenge_json.get("data", {}).get("content", [])
    if not content:
        print("[upload-smoke] no challenge found for smoke test", file=sys.stderr)
        return 2

    challenge = content[0]
    challenge_id = challenge["challengeId"]
    member_user_id = challenge["leader"]["userId"]
    non_member_user_id = str(uuid.uuid4())
    member_token = make_token(member_user_id, secret)
    non_member_token = make_token(non_member_user_id, secret)

    # assets
    feed_ok = assets_dir / "feed_ok.jpg"
    make_jpeg(feed_ok, 900, 600, "feed ok")

    feed_over = assets_dir / "feed_over_20mb.jpg"
    make_large_file(feed_over, 21 * 1024 * 1024)

    banner_bad_res = assets_dir / "banner_bad_res.jpg"
    make_jpeg(banner_bad_res, 2048, 1151, "banner bad res")

    thumb_bad_res = assets_dir / "thumb_bad_res.jpg"
    make_jpeg(thumb_bad_res, 127, 127, "thumb bad res")

    profile_bad_res = assets_dir / "profile_bad_res.jpg"
    make_jpeg(profile_bad_res, 127, 127, "profile bad res")

    rows: list[ResultRow] = []

    def run_case(
        case_id: str,
        expected: str,
        method: str,
        path: str,
        token: str,
        files: list[tuple[str, tuple[str, bytes, str]]],
        expected_status: int,
        expected_message_prefix: str | None = None,
    ) -> None:
        status, body, raw = call_multipart(session, method, f"{args.base_url}{path}", token, files)
        message = body.get("message", "") if isinstance(body, dict) else ""
        passed = status == expected_status
        if expected_message_prefix is not None:
            passed = passed and isinstance(message, str) and message.startswith(expected_message_prefix)
        evidence_file = evidence_dir / f"{case_id}.json"
        evidence_file.write_text(
            json.dumps({"status": status, "body": body, "raw": raw}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        rows.append(
            ResultRow(
                case_id=case_id,
                expected=expected,
                actual=f"status={status}, message={message}",
                status_code=status,
                api_message=message,
                passed=passed,
                evidence=str(evidence_file),
            )
        )

    run_case(
        case_id="POST_IMAGE_OK_1",
        expected="200 + imageUrls >= 1",
        method="POST",
        path=f"/challenges/{challenge_id}/posts/images",
        token=member_token,
        files=[("files", (feed_ok.name, feed_ok.read_bytes(), "image/jpeg"))],
        expected_status=200,
    )

    files_11 = [("files", (f"feed_{idx}.jpg", feed_ok.read_bytes(), "image/jpeg")) for idx in range(1, 12)]
    run_case(
        case_id="POST_IMAGE_11_FILES",
        expected="400 + IMAGE_004",
        method="POST",
        path=f"/challenges/{challenge_id}/posts/images",
        token=member_token,
        files=files_11,
        expected_status=400,
        expected_message_prefix="IMAGE_004",
    )

    run_case(
        case_id="POST_IMAGE_OVER_20MB",
        expected="400 + IMAGE_002",
        method="POST",
        path=f"/challenges/{challenge_id}/posts/images",
        token=member_token,
        files=[("files", (feed_over.name, feed_over.read_bytes(), "image/jpeg"))],
        expected_status=400,
        expected_message_prefix="IMAGE_002",
    )

    run_case(
        case_id="POST_IMAGE_NON_MEMBER",
        expected="403 + MEMBER_001",
        method="POST",
        path=f"/challenges/{challenge_id}/posts/images",
        token=non_member_token,
        files=[("files", (feed_ok.name, feed_ok.read_bytes(), "image/jpeg"))],
        expected_status=403,
        expected_message_prefix="MEMBER_001",
    )

    run_case(
        case_id="BANNER_BAD_RES",
        expected="400 + IMAGE_003",
        method="POST",
        path="/uploads/challenges/banner",
        token=member_token,
        files=[("file", (banner_bad_res.name, banner_bad_res.read_bytes(), "image/jpeg"))],
        expected_status=400,
        expected_message_prefix="IMAGE_003",
    )

    run_case(
        case_id="THUMB_BAD_RES",
        expected="400 + IMAGE_003",
        method="POST",
        path="/uploads/challenges/thumbnail",
        token=member_token,
        files=[("file", (thumb_bad_res.name, thumb_bad_res.read_bytes(), "image/jpeg"))],
        expected_status=400,
        expected_message_prefix="IMAGE_003",
    )

    run_case(
        case_id="PROFILE_BAD_RES",
        expected="400 + IMAGE_003",
        method="POST",
        path="/users/me/profile-image",
        token=member_token,
        files=[("file", (profile_bad_res.name, profile_bad_res.read_bytes(), "image/jpeg"))],
        expected_status=400,
        expected_message_prefix="IMAGE_003",
    )

    write_outputs(
        out_dir,
        rows,
        metadata={
            "challengeId": challenge_id,
            "memberUserId": member_user_id,
            "baseUrl": args.base_url,
        },
    )

    for row in rows:
        status = "PASS" if row.passed else "FAIL"
        print(f"[upload-smoke] {status} {row.case_id} :: {row.actual}")

    failed = [row for row in rows if not row.passed]
    print(f"[upload-smoke] results: pass={len(rows) - len(failed)} fail={len(failed)}")
    print(f"[upload-smoke] output: {out_dir}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
