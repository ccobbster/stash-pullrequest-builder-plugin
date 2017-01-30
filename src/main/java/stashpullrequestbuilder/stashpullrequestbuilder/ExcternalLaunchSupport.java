package stashpullrequestbuilder.stashpullrequestbuilder;

import java.util.HashMap;
import java.util.Map;

import hudson.model.AbstractBuild;
import stashpullrequestbuilder.stashpullrequestbuilder.stash.StashPullRequestResponseValue;
import stashpullrequestbuilder.stashpullrequestbuilder.stash.StashPullRequestResponseValueRepository;

public class ExcternalLaunchSupport {
    private static final String PULL_REQUEST_ID = "pullRequestId";
    private static final String PULL_REQUEST_VERSION = "pullRequestVersion";
    private static final String PULL_REQUEST_TITLE = "pullRequestTitle";
    private static final String TARGET_BRANCH = "targetBranch";
    private static final String DESTINATION_COMMIT_HASH = "destinationCommitHash";
    private static final String DESTINATION_REPOSITORY_NAME = "destinationRepositoryName";
    private static final String DESTINATION_REPOSITORY_OWNER = "destinationRepositoryOwner";
    private static final String SOURCE_BRANCH = "sourceBranch";
    private static final String SOURCE_COMMIT_HASH = "sourceCommitHash";
    private static final String SOURCE_REPOSITORY_NAME = "sourceRepositoryName";
    private static final String SOURCE_REPOSITORY_OWNER = "sourceRepositoryOwner";

    private static Map<String, String> PULL_REQUEST_NOTIFIER_FOR_BITBUCKET_MAP = new HashMap<String, String>() {
        {
            put(PULL_REQUEST_ID, "PULL_REQUEST_ID");
            put(PULL_REQUEST_TITLE, "PULL_REQUEST_TITLE");
            put(PULL_REQUEST_VERSION, "PULL_REQUEST_VERSION");
            put(SOURCE_BRANCH, "PULL_REQUEST_FROM_BRANCH");
            put(SOURCE_REPOSITORY_OWNER, "PULL_REQUEST_FROM_REPO_PROJECT_KEY");
            put(SOURCE_REPOSITORY_NAME, "PULL_REQUEST_FROM_REPO_SLUG");
            put(SOURCE_COMMIT_HASH, "PULL_REQUEST_FROM_HASH");
            put(TARGET_BRANCH, "PULL_REQUEST_TO_BRANCH");
            put(DESTINATION_REPOSITORY_OWNER, "PULL_REQUEST_TO_REPO_PROJECT_KEY");
            put(DESTINATION_REPOSITORY_NAME, "PULL_REQUEST_TO_REPO_SLUG");
            put(DESTINATION_COMMIT_HASH, "PULL_REQUEST_TO_HASH");
        }
    };

    private static Map<String, String> STASH_PULLREQUEST_BUILDER_PLUGIN_MAP = new HashMap<String, String>() {
        {
            put(PULL_REQUEST_ID, PULL_REQUEST_ID);
            put(PULL_REQUEST_TITLE, PULL_REQUEST_TITLE);
            put(PULL_REQUEST_VERSION, PULL_REQUEST_VERSION);
            put(SOURCE_BRANCH, SOURCE_BRANCH);
            put(SOURCE_REPOSITORY_OWNER, SOURCE_REPOSITORY_OWNER);
            put(SOURCE_REPOSITORY_NAME, SOURCE_REPOSITORY_NAME);
            put(SOURCE_COMMIT_HASH, SOURCE_COMMIT_HASH);
            put(TARGET_BRANCH, TARGET_BRANCH);
            put(DESTINATION_REPOSITORY_OWNER, DESTINATION_REPOSITORY_OWNER);
            put(DESTINATION_REPOSITORY_NAME, DESTINATION_REPOSITORY_NAME);
            put(DESTINATION_COMMIT_HASH, DESTINATION_COMMIT_HASH);
        }
    };

    private static String getStringVariableValue(Map<String, String> vars, Map<String, String> keymap, String key) {
        Object value = vars.get(keymap.get(key));
        return value == null ? null : value.toString();
    }

    public static StashCause resolveOnStartedStashCause(final StashCause cause, final StashRepository repository,
                                                        final StashBuildTrigger trigger,
                                                        final AbstractBuild<?, ?> build) {
        if (cause != null)
            return cause;

        final Map<String, String> vars = build.getBuildVariables();

        final Map<String, String> keymap;
        if (vars.containsKey(PULL_REQUEST_ID)) {
            keymap = STASH_PULLREQUEST_BUILDER_PLUGIN_MAP;
        } else if (vars.containsKey(PULL_REQUEST_NOTIFIER_FOR_BITBUCKET_MAP.get(PULL_REQUEST_ID))) {
            keymap = PULL_REQUEST_NOTIFIER_FOR_BITBUCKET_MAP;
        } else
            return null;

        StashPullRequestResponseValue pullRequest = new StashPullRequestResponseValue();
        pullRequest.setToRef(new StashPullRequestResponseValueRepository());
        pullRequest.getToRef().setLatestCommit(getStringVariableValue(vars, keymap, DESTINATION_COMMIT_HASH));
        pullRequest.setFromRef(new StashPullRequestResponseValueRepository());
        pullRequest.getFromRef().setLatestCommit(getStringVariableValue(vars, keymap, SOURCE_COMMIT_HASH));
        pullRequest.setId(getStringVariableValue(vars, keymap, PULL_REQUEST_ID));
        repository.init();
        String commentId = repository.postBuildStartCommentTo(pullRequest);

        StashCause newCause = new StashCause(trigger.getStashHost(),
                getStringVariableValue(vars, keymap, SOURCE_BRANCH),
                getStringVariableValue(vars, keymap, TARGET_BRANCH),
                getStringVariableValue(vars, keymap, SOURCE_REPOSITORY_OWNER),
                getStringVariableValue(vars, keymap, SOURCE_REPOSITORY_NAME),
                getStringVariableValue(vars, keymap, PULL_REQUEST_ID),
                getStringVariableValue(vars, keymap, DESTINATION_REPOSITORY_OWNER),
                getStringVariableValue(vars, keymap, DESTINATION_REPOSITORY_NAME),
                getStringVariableValue(vars, keymap, PULL_REQUEST_TITLE),
                getStringVariableValue(vars, keymap, SOURCE_COMMIT_HASH),
                getStringVariableValue(vars, keymap, DESTINATION_COMMIT_HASH),
                commentId,
                new HashMap<String, String>());

        build.addAction(new StashCauseWrapperAction(newCause));

        return newCause;
    }

    public static StashCause resolveOnCompletedStashCause(final StashCause cause, final AbstractBuild<?, ?> build) {
        if (cause != null)
            return cause;
        StashCauseWrapperAction causeWrapperAction = build.getAction(StashCauseWrapperAction.class);
        return causeWrapperAction == null ? cause : causeWrapperAction.getStashCause();
    }

}
