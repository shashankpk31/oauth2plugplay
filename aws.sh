# 1. Get the latest Sonnet/Opus Inference Profile ID
export ANTHROPIC_MODEL=$(aws bedrock list-inference-profiles \
    --region us-east-1 \
    --query "inferenceProfileSummaries[?contains(inferenceProfileName, 'Sonnet') || contains(inferenceProfileName, 'Opus')].inferenceProfileId" \
    --output text | tr '\t' '\n' | sort -r | head -n 1)

# 2. Get the fastest (Haiku) Inference Profile ID
export ANTHROPIC_SMALL_FAST_MODEL=$(aws bedrock list-inference-profiles \
    --region us-east-1 \
    --query "inferenceProfileSummaries[?contains(inferenceProfileName, 'Haiku')].inferenceProfileId" \
    --output text | tr '\t' '\n' | sort -r | head -n 1)

# 3. Set the core Bedrock flags
export CLAUDE_CODE_USE_BEDROCK=1
export AWS_REGION="us-east-1"

# Print results to verify
echo "LATEST PROFILE: $ANTHROPIC_MODEL"
echo "FASTEST PROFILE: $ANTHROPIC_SMALL_FAST_MODEL"