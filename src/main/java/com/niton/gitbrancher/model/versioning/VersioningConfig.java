package com.niton.gitbrancher.model.versioning;

import java.io.Serializable;
import java.util.*;

import static com.niton.gitbrancher.model.versioning.ReleaseStep.*;

public class VersioningConfig implements Serializable {
    /**
     * Seperator used to print the version
     */
    public static String
        releasePartSeperator = ".",
        stateSeperator = "-",
        differencingSeperator = "_";
    private VersioningSchema numberSchema = VersioningSchema.NORMAL;
    /**
     * This is a additional (custom) parameter which in the version declaration displays a custom build (target platform or delivery format)
     *
     * Setting it to null disables differencations
     */
    private String differenciation = null;
    /**
     * This map holds the information which state is replaced by wich term in the version code
     */
    private Map<ReleaseState,String> releaseStateReplacementMap = new HashMap<>();
    private Map<ReleaseState,Boolean> releaseStateUsageMap = new HashMap<>();

    /**
     * This list contains all {@link ReleaseStep}s that have a seperate branch for your seperation {@link #differenciation}
     */
    private List<ReleaseStep> differencationInfluenced = List.of(MAYOR,MINOR);

    VersioningConfig(){
        Arrays.stream(ReleaseState.values()).forEach((state)->{
            releaseStateReplacementMap.put(state, state.getDefaultReplacement());
            releaseStateUsageMap.put(state, state.isUsedByDefault());
        });
    }

    public static String getReleasePartSeperator() {
        return releasePartSeperator;
    }

    public static void setReleasePartSeperator(String releasePartSeperator) {
        VersioningConfig.releasePartSeperator = releasePartSeperator;
    }

    public static String getStateSeperator() {
        return stateSeperator;
    }

    public static void setStateSeperator(String stateSeperator) {
        VersioningConfig.stateSeperator = stateSeperator;
    }

    public static String getDifferencingSeperator() {
        return differencingSeperator;
    }

    public static void setDifferencingSeperator(String differencingSeperator) {
        VersioningConfig.differencingSeperator = differencingSeperator;
    }

    public VersioningSchema getNumberSchema() {
        return numberSchema;
    }

    public void setNumberSchema(VersioningSchema numberSchema) {
        this.numberSchema = numberSchema;
    }

    public Map<ReleaseState, String> getReleaseStateReplacementMap() {
        return releaseStateReplacementMap;
    }

    public Map<ReleaseState, Boolean> getReleaseStateUsageMap() {
        return releaseStateUsageMap;
    }

    public List<ReleaseStep> getDifferencationInfluenced() {
        return differencationInfluenced;
    }

    public String getDifferenciation() {
        return differenciation;
    }

    public void setDifferenciation(String differenciation) {
        this.differenciation = differenciation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersioningConfig that = (VersioningConfig) o;
        return numberSchema == that.numberSchema &&
                Objects.equals(differenciation, that.differenciation) &&
                releaseStateReplacementMap.equals(that.releaseStateReplacementMap) &&
                releaseStateUsageMap.equals(that.releaseStateUsageMap) &&
                differencationInfluenced.equals(that.differencationInfluenced);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberSchema, differenciation, releaseStateReplacementMap, releaseStateUsageMap, differencationInfluenced);
    }

    public String getReplacement(ReleaseState state) {
        return releaseStateReplacementMap.get(state);
    }

}
