package org.broadinstitute.sting.playground.gatk.walkers.variants;

import org.broadinstitute.sting.gatk.contexts.VariantContext;

public class VECLodThreshold implements VariantExclusionCriterion {
    private double lodThreshold = 5.0;
    private double lod;
    private boolean exclude;

    public void initialize(String arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            lodThreshold = Double.valueOf(arguments);
        }
    }

    public void compute(VariantContextWindow contextWindow) {
        VariantContext context = contextWindow.getContext();
        lod = context.getVariant().getLodBtr();
        exclude = lod < lodThreshold;
    }

    public boolean useZeroQualityReads() { return false; }

    public double inclusionProbability() {
        // A hack for now until this filter is actually converted to an empirical filter
        return exclude ? 0.0 : 1.0;
    }

    public String getStudyHeader() {
        return "LodThreshold("+lod+")\tlod";
    }

    public String getStudyInfo() {
        return (exclude ? "fail" : "pass") + "\t" + lod;
    }

}
