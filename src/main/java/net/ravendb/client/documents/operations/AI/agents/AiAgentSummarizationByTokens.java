package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentSummarizationByTokens {
    private String summarizationTaskBeginningPrompt;
    private String summarizationTaskEndPrompt;
    private String resultPrefix;
    private Integer maxTokensBeforeSummarization;
    private Integer maxTokensAfterSummarization;

    public AiAgentSummarizationByTokens() {
    }

    public String getSummarizationTaskBeginningPrompt() {
        return summarizationTaskBeginningPrompt;
    }

    public void setSummarizationTaskBeginningPrompt(String summarizationTaskBeginningPrompt) {
        this.summarizationTaskBeginningPrompt = summarizationTaskBeginningPrompt;
    }

    public String getSummarizationTaskEndPrompt() {
        return summarizationTaskEndPrompt;
    }

    public void setSummarizationTaskEndPrompt(String summarizationTaskEndPrompt) {
        this.summarizationTaskEndPrompt = summarizationTaskEndPrompt;
    }

    public String getResultPrefix() {
        return resultPrefix;
    }

    public void setResultPrefix(String resultPrefix) {
        this.resultPrefix = resultPrefix;
    }

    public Integer getMaxTokensBeforeSummarization() {
        return maxTokensBeforeSummarization;
    }

    public void setMaxTokensBeforeSummarization(Integer maxTokensBeforeSummarization) {
        this.maxTokensBeforeSummarization = maxTokensBeforeSummarization;
    }

    public Integer getMaxTokensAfterSummarization() {
        return maxTokensAfterSummarization;
    }

    public void setMaxTokensAfterSummarization(Integer maxTokensAfterSummarization) {
        this.maxTokensAfterSummarization = maxTokensAfterSummarization;
    }
}
