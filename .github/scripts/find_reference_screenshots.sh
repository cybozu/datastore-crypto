#!/bin/bash
set -e

# 引数の確認
WORKFLOW_NAME="$1"
ARTIFACT_NAME="$2"

if [ -z "$WORKFLOW_NAME" ] || [ -z "$ARTIFACT_NAME" ]; then
  echo "Usage: $0 <workflow_name> <artifact_name>"
  echo "found=false" >> "$GITHUB_OUTPUT"
  exit 1
fi

# デフォルトブランチで成功した最新のworkflow実行を検索
DEFAULT_BRANCH="${GITHUB_REPOSITORY_DEFAULT_BRANCH}"
echo "Repository default branch: $DEFAULT_BRANCH"
echo "Searching for successful workflow runs for '$WORKFLOW_NAME' on branch '$DEFAULT_BRANCH'..."

# ワークフロー名からワークフローIDを取得
workflow_info=$(gh api -X GET repos/$GITHUB_REPOSITORY/actions/workflows \
  -f per_page=100 | jq -r '.workflows[] | select(.name=="'"$WORKFLOW_NAME"'") | .id')

if [ -z "$workflow_info" ]; then
  echo "No workflow named '$WORKFLOW_NAME' found"
  echo "found=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

workflow_id=$workflow_info
echo "Found workflow ID: $workflow_id"

# ワークフローIDを使って最新の成功実行を検索
run_info=$(gh api -X GET repos/$GITHUB_REPOSITORY/actions/workflows/"$workflow_id"/runs \
  -f branch=$DEFAULT_BRANCH -f status=success -f per_page=1 | jq -r 'if .workflow_runs and (.workflow_runs | length > 0) then .workflow_runs[0] | {id, created_at} else null end')

if [ -z "$run_info" ] || [ "$run_info" = "null" ]; then
  echo "No successful workflow runs found for '$WORKFLOW_NAME' on branch '$DEFAULT_BRANCH'"
  echo "found=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

run_id=$(echo "$run_info" | jq -r '.id')
created_at=$(echo "$run_info" | jq -r '.created_at')

echo "Found workflow run ID: $run_id (created at $created_at)"

# すべてのアーティファクトをリストして詳細を確認
echo "Listing all artifacts for run ID $run_id:"
gh api -X GET repos/$GITHUB_REPOSITORY/actions/runs/"$run_id"/artifacts -f per_page=100 | jq -r '.artifacts[] | "  - \(.name) (ID: \(.id), Expired: \(.expired), Size: \(.size_in_bytes) bytes)"'

# Artifactの存在を確認
artifacts_info=$(gh api -X GET repos/$GITHUB_REPOSITORY/actions/runs/"$run_id"/artifacts -f per_page=100 | jq -r '.artifacts[] | select(.name=="'"$ARTIFACT_NAME"'") | {id, expired, size_in_bytes}')

if [ -z "$artifacts_info" ]; then
  echo "No '$ARTIFACT_NAME' artifact found in run $run_id"
  echo "found=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

artifact_id=$(echo "$artifacts_info" | jq -r '.id')
artifact_expired=$(echo "$artifacts_info" | jq -r '.expired')
artifact_size=$(echo "$artifacts_info" | jq -r '.size_in_bytes')

echo "Found '$ARTIFACT_NAME' artifact in run $run_id (ID: $artifact_id, Expired: $artifact_expired, Size: $artifact_size bytes)"

if [ "$artifact_expired" = "true" ]; then
  echo "Warning: Artifact is marked as expired!"
  echo "found=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

if [ "$artifact_size" -le 0 ]; then
  echo "Warning: Artifact has zero or negative size!"
  echo "found=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

# 結果を返す
echo "found=true" >> "$GITHUB_OUTPUT"
echo "run_id=$run_id" >> "$GITHUB_OUTPUT" 