import json
from datetime import date, datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import Any, Optional

from django.conf import settings
from django.http import HttpRequest, JsonResponse
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt

Q4 = Decimal("0.0001")


def _to_decimal(value: Any) -> Decimal:
    if value is None:
        return Decimal("0")
    try:
        return Decimal(str(value))
    except Exception:
        return Decimal("0")


def _round4(value: Decimal) -> Decimal:
    return value.quantize(Q4, rounding=ROUND_HALF_UP)


def _to_long(value: Any) -> int:
    if value is None:
        return 0
    try:
        return int(Decimal(str(value)))
    except Exception:
        return 0


def _parse_datetime(value: Any) -> Optional[datetime]:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    try:
        parsed = datetime.fromisoformat(text)
    except ValueError:
        return None
    if parsed.tzinfo is not None:
        return parsed.astimezone(timezone.get_current_timezone()).replace(tzinfo=None)
    return parsed


def _add_months(source: date, diff: int) -> date:
    month_index = (source.month - 1) + diff
    year = source.year + (month_index // 12)
    month = (month_index % 12) + 1
    return date(year, month, 1)


@csrf_exempt
def calculate_brix(request: HttpRequest) -> JsonResponse:
    if request.method != "POST":
        return JsonResponse({"message": "Method not allowed"}, status=405)

    api_key = request.headers.get("X-Api-Key")
    if api_key != settings.INTERNAL_API_KEY:
        return JsonResponse({"message": "Unauthorized"}, status=401)

    try:
        body = json.loads(request.body.decode("utf-8"))
    except Exception:
        return JsonResponse({"message": "Invalid JSON body"}, status=400)

    users = body.get("users")
    if not isinstance(users, list):
        return JsonResponse({"message": "users must be an array"}, status=400)

    results = []
    for user in users:
        if not isinstance(user, dict):
            continue
        user_id = user.get("userId")
        if not user_id:
            continue

        attendance = _to_decimal(user.get("attendance"))
        payment_months = _to_decimal(user.get("paymentMonths"))
        overdue = _to_decimal(user.get("overdue"))
        consecutive_overdue = _to_decimal(user.get("consecutiveOverdue"))
        feed = _to_decimal(user.get("feed"))
        comment = _to_decimal(user.get("comment"))
        like = _to_decimal(user.get("like"))
        leader_months = _to_decimal(user.get("leaderMonths"))
        vote_absence = _to_decimal(user.get("voteAbsence"))
        report_received = _to_decimal(user.get("reportReceived"))
        kick_count = _to_decimal(user.get("kickCount"))

        payment_score = (
            attendance * Decimal("0.09")
            + payment_months * Decimal("0.32")
            - overdue * Decimal("1.5")
            - consecutive_overdue * Decimal("1.0")
        )
        activity_score = (
            feed * Decimal("0.05")
            + comment * Decimal("0.025")
            + like * Decimal("0.006")
            + leader_months * Decimal("0.45")
            - vote_absence * Decimal("0.1")
            - report_received * Decimal("0.6")
            - kick_count * Decimal("4.0")
        )

        total = Decimal("12") + (payment_score * Decimal("0.7")) + (activity_score * Decimal("0.15"))
        if total > Decimal("80"):
            total = Decimal("80")

        payment_score = _round4(payment_score)
        activity_score = _round4(activity_score)
        total = _round4(total)

        results.append(
            {
                "userId": user_id,
                "paymentScore": float(payment_score),
                "activityScore": float(activity_score),
                "totalScore": float(total),
            }
        )

    return JsonResponse(
        {
            "calculatedAt": timezone.now().isoformat(),
            "results": results,
        },
        status=200,
    )


@csrf_exempt
def calculate_ledger_chart(request: HttpRequest) -> JsonResponse:
    if request.method != "POST":
        return JsonResponse({"message": "Method not allowed"}, status=405)

    api_key = request.headers.get("X-Api-Key")
    if api_key != settings.INTERNAL_API_KEY:
        return JsonResponse({"message": "Unauthorized"}, status=401)

    try:
        body = json.loads(request.body.decode("utf-8"))
    except Exception:
        return JsonResponse({"message": "Invalid JSON body"}, status=400)

    months = body.get("months")
    try:
        months = int(months) if months is not None else 6
    except Exception:
        months = 6
    months = max(1, min(24, months))

    current_balance = _to_long(body.get("currentBalance"))
    entries = body.get("entries")
    if not isinstance(entries, list):
        entries = []

    now_local = timezone.localtime()
    current_month_start = date(now_local.year, now_local.month, 1)
    first_month_start = _add_months(current_month_start, -(months - 1))
    month_keys = [_add_months(first_month_start, i).strftime("%Y-%m") for i in range(months)]

    expenses_map = {month_key: 0 for month_key in month_keys}
    month_end_balance_map = {month_key: None for month_key in month_keys}
    month_end_seen_at = {month_key: None for month_key in month_keys}

    for entry in entries:
        if not isinstance(entry, dict):
            continue
        created_at = _parse_datetime(entry.get("createdAt"))
        if created_at is None:
            continue

        month_key = created_at.strftime("%Y-%m")
        if month_key not in expenses_map:
            continue

        entry_type = str(entry.get("type") or "").upper()
        amount = _to_long(entry.get("amount"))
        if entry_type == "EXPENSE":
            expenses_map[month_key] += abs(amount)

        balance_after = entry.get("balanceAfter")
        if balance_after is not None:
            previous_seen_at = month_end_seen_at[month_key]
            if previous_seen_at is None or created_at >= previous_seen_at:
                month_end_balance_map[month_key] = _to_long(balance_after)
                month_end_seen_at[month_key] = created_at

    current_month_key = current_month_start.strftime("%Y-%m")
    if current_month_key in month_end_balance_map:
        month_end_balance_map[current_month_key] = current_balance

    raw_balances = [month_end_balance_map[month_key] for month_key in month_keys]
    filled_balances = []
    last_seen = None
    for value in raw_balances:
        if value is None:
            filled_balances.append(last_seen if last_seen is not None else None)
        else:
            last_seen = value
            filled_balances.append(value)

    if filled_balances:
        filled_balances[-1] = current_balance

    monthly_expenses = [
        {"month": month_key, "expense": int(expenses_map[month_key])}
        for month_key in month_keys
    ]
    monthly_balances = [
        {
            "month": month_keys[index],
            "balance": int(filled_balances[index]) if filled_balances[index] is not None else None,
        }
        for index in range(len(month_keys))
    ]

    return JsonResponse(
        {
            "calculatedAt": timezone.now().isoformat(),
            "monthlyExpenses": monthly_expenses,
            "monthlyBalances": monthly_balances,
        },
        status=200,
    )
