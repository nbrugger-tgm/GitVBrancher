package com.niton.gitbrancher.model.versioning;

public enum ReleaseStep {
	MAYOR(null),
	MINOR(MAYOR),
	PATCH(MINOR),
	REVISION(PATCH);
	public final static ReleaseStep smallest = REVISION;

	/**
	 * Defines the ReleaseStep this step is underlying, or in other words defines the next bigger stepss
	 */
	private final ReleaseStep superior;

	ReleaseStep(ReleaseStep patch) {
		this.superior = patch;
	}

	public ReleaseStep getSuperior() {
		return superior;
	}
}
