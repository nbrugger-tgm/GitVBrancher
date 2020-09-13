package com.niton.gitbrancher.model.versioning;

import com.niton.gitbrancher.model.algo.TwoGradeIdMap;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class VersionNumber implements Serializable {
	private Map<ReleaseStep,Integer> data;

	public VersionNumber(Map<ReleaseStep, Integer> data) {
		this.data = data;
	}

	public VersionNumber() {
		data = new HashMap<>();
	}

	public Integer getNumber(ReleaseStep o) {
		return data.get(o);
	}

	public Integer setNumber(ReleaseStep releaseStep, Integer integer) {
		return data.put(releaseStep, integer);
	}

	public Set<ReleaseStep> steps() {
		return data.keySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VersionNumber that = (VersionNumber) o;
		return Objects.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(data);
	}


	public VersionNumber getBaseVersion(VersioningConfig schema){
		Map<ReleaseStep,Integer> baseCode = new HashMap<>();
		ReleaseStep curStep = ReleaseStep.smallest;
		boolean found = false;
		while(curStep.getSuperior() != null){
			//necessary for interface/lambda
			ReleaseStep finalCurStep = curStep;
			if(Arrays.stream(schema.getNumberSchema().getReleaseChunks()).anyMatch(e -> e == finalCurStep)){
				if (found){
					baseCode.put(curStep,getNumber(curStep));
				}
				else if (getNumber(curStep) != 0){
					found = true;
					baseCode.put(curStep,getNumber(curStep)-1);
				}
			}
			curStep = curStep.getSuperior();
		}
		return new VersionNumber(Collections.unmodifiableMap(baseCode));
	}
	public String getNotation(VersioningConfig schema) {
		return Arrays.stream(schema.getNumberSchema().getReleaseChunks())
				.map(e -> Integer.toString(getNumber(e)))
				.collect(Collectors.joining(VersioningConfig.getReleasePartSeperator()));
	}

	@Override
	public String toString() {
		return getNotation(new VersioningConfig());
	}
}
