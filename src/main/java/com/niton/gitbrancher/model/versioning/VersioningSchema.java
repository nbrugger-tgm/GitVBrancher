package com.niton.gitbrancher.model.versioning;

import static com.niton.gitbrancher.model.versioning.ReleaseStep.*;

public enum VersioningSchema {
	SIMPLE(MAYOR, MINOR),
	NORMAL(MAYOR, MINOR, PATCH),
	DETAILED(MAYOR, MINOR, PATCH, REVISION);
	private final ReleaseStep[] releaseChunks;

	VersioningSchema(ReleaseStep... releaseChunk) {
		this.releaseChunks = releaseChunk;
	}

	public ReleaseStep[] getReleaseChunks() {
		return releaseChunks;
	}
}
