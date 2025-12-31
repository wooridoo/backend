# 깃 허브 세팅

1. 해당 Repository 선택 -> Settings
  - General -> Pull Requests -> "Automatically delete head branches" 체크
  - Actions -> General -> Workflow permissions -> "Read and write permissions" 체크
    - 바로 아래에 있는 "Allow GitHub Actions to create and approve pull requests" 체크
  - Secrets and variables -> Actions -> Repository secrets에 토큰, URL, Email 생성 (JIRA_API_TOKEN, JIRA_BASE_URL, JIRA_USER_EMAIL)
  - 공유받은 멤버들이 해당 레포지토리의 **Collaborator(협력자)**로 등록되어 있고, 권한이 Write 이상이어야 합니다.
---
2. 메인 브랜치에 템플릿, 워크플로우 파일 생성 (.yml 파일)

  - 템플릿 위치 주소 <br>
    .github/ISSUE_TEMPLATE/issue-form.yml

  - 워크플로우 위치 주소 <br>
    .github/workflows/close-jira-on-merge.yml <br>
    .github/workflows/create-jira-issue.yml <br>
---
### **확인해야 할 내용**
  - Setting에서 확인
    - secrets.JIRA_BASE_URL
    - secrets.JIRA_API_TOKEN
    - secrets.JIRA_USER_EMAIL

토큰 생성<br>
https://id.atlassian.com/manage-profile/security/api-tokens
<br>지라 토큰은 해당 프로젝트에 편집 권한이 있는 계정이어야 함


### **바꿔야 하는 코드**
  - 템플릿 주소 확인
    name: Issue Parser => template-path: .github/ISSUE_TEMPLATE/issue-form.yml

  - 내용 지라에 맞게 수정
	  - name: Create Jira Issue => project: KAN, issuetype: Task
	    - project = 티켓 이름(숫자 제외) ex) WRD-01
	    - issuetype = 생성할 업무 유형 ex) Epic, Task, 에픽, 작업 (***영어***, ***한글*** 구분 확실히)
      - name: Close Jira issue => transition: DONE (DONE, 완료 등 지라 세팅에 맞게)

<br>

---
---


# 지라 세팅

- github - jira 연동
  - Github for Atlassian 설치 후 구성에서 Repository 연결
  - 에픽으로 이슈를 모아놓을 상위 업무 생성 (해당 티켓으로 )
- 현재 업무의 상태 이름을 확인
  - Done, 완료 (영문, 한글 구분)

<br>

---
---

# 멤버 주의사항

- 이슈 템플릿 작성 시: parentKey란에 상위 에픽 번호(예: KAN-1)를 정확히 입력해야 지라 티켓이 생성됩니다.

- 브랜치 이동: 이슈 생성 후 10~20초 뒤에 봇이 브랜치를 자동으로 만듭니다. 본인이 직접 브랜치를 만들지 말고, git fetch 후 자동 생성된 브랜치(KAN-번호-명칭)에서 작업을 시작하세요.

- PR 본문 유지: 자동 생성된 PR 본문의 #GitHub이슈번호를 지우면 머지 후 이슈가 자동으로 닫히지 않습니다.

<br>

---
---

## 깃 액션의 흐름 (예시)

1. 이슈 생성 (자동화)
    - 상황: main 브랜치에 A.java가 있는 상태.

    - 행동: GitHub 이슈에 "A를 BC로 수정" 등록.

    - 결과: 봇이 Jira 티켓 생성 + KAN-1-fix-A 브랜치 생성.


2. 팀원 작업 (로컬)
    - 가져오기: git fetch 후 git checkout KAN-1-fix-A로 이동.

    - 수정/제출: 코드 수정 후 git add → commit → push.


3. PR 요청 (검토 신청)
    - 행동: GitHub에서 해당 브랜치로 Pull Request 버튼 클릭.

    - 의미: "작업 완료! 내 코드를 검토하고 main에 합쳐주세요"라는 공식 선언.


4. 승인 및 종료 (자동화)
    - 검토: 관리자가 코드 확인 후 Merge 버튼 클릭.

    - 결과: main에 코드 반영 + Jira 티켓 & GitHub 이슈 자동 종료.

<br>

**봇이 만든 브랜치로 바로 가는 법 <br>**
  git fetch <br>
  git checkout [탭 키를 눌러 자동완성]
