package com.niton.gitbrancher.model.versioning;

public enum ReleaseState {
	//ordering is important for the compiler
	RELEASED(true, null, null),
	PRE_RELEASE(false, "pre", RELEASED),
	BETA(true, "SNAPSHOT", PRE_RELEASE),
	TESTING(true, "testing", BETA),
	DEVELOPMENT(true, "dev", TESTING);

	/**
	 * The text used to display this state in the notation by default
	 */
	private final String defaultReplacement;
	/**
	 * If this is a state used by a default {@link VersioningConfig} instance
	 */
	private final boolean usedByDefault;
	/**
	 * The state entered when this state is completed
	 */
	private final ReleaseState mergeTarget;

	ReleaseState(boolean usedByDefault, String defaultReplacement, ReleaseState mergeOnSuccess) {
		this.usedByDefault = usedByDefault;
		this.defaultReplacement = defaultReplacement;
		this.mergeTarget = mergeOnSuccess;
	}

	public String getDefaultReplacement() {
		return defaultReplacement;
	}

	public boolean isUsedByDefault() {
		return usedByDefault;
	}

	public ReleaseState getMergeTarget() {
		return mergeTarget;
	}
	public static ReleaseState getStartingState(){
		return DEVELOPMENT;
	}
}
