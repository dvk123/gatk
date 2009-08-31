package org.broadinstitute.sting.playground.gatk.walkers.variants;

import org.broadinstitute.sting.gatk.contexts.VariantContext;
import org.broadinstitute.sting.utils.MathUtils;
import org.broadinstitute.sting.utils.ReadBackedPileup;

public class IVFSecondaryBases implements IndependentVariantFeature {
    private double[] p2on  = { 0.000, 0.302, 0.366, 0.142, 0.000, 0.548, 0.370, 0.000, 0.319, 0.000 };
    private double[] p2off = { 0.480, 0.769, 0.744, 0.538, 0.575, 0.727, 0.768, 0.589, 0.762, 0.505 };

    private double[] likelihoods;

    /**
     * Method so that features can initialize themselves based on a short argument string. At the moment, each feature is
     * responsible for interpreting their own argument string.
     *
     * @param arguments the arguments!
     */
    public void initialize(String arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            String[] argPieces = arguments.split(";");
            String[] argOnPieces = argPieces[0].split(",");
            String[] argOffPieces = argPieces[0].split(",");

            for (int genotypeIndex = 0; genotypeIndex < 10; genotypeIndex++) {
                p2on[genotypeIndex] = Double.valueOf(argOnPieces[genotypeIndex]);
                p2off[genotypeIndex] = Double.valueOf(argOffPieces[genotypeIndex]);
            }
        }
    }

    private double scaleObservation(int k, int N, double p) {
        int i = 0;
        double likelihood = 0.0, logLikelihood = 0.0, scale = 1.0;

        do {
            scale = 1.0 - ((double) i)*0.10;

            int scaledK = (int) (scale * ((double) k));
            int scaledN = (int) (scale * ((double) N));

            likelihood = MathUtils.binomialProbability(scaledK, scaledN, p);
            logLikelihood = Math.log10(likelihood);

            //System.out.printf("  %d %f: %d->%d, %d->%d, %f => %f %f\n", i, scale, k, scaledK, N, scaledN, p, likelihood, logLikelihood);

            i++;
        } while (i < 10 && (Double.isInfinite(logLikelihood) || Double.isNaN(logLikelihood)));

        //System.out.println();

        return likelihood;
    }

    /**
     * Method to compute the result of this feature for each of the ten genotypes.  The return value must be a double array
     * of length 10 (one for each genotype) and the value must be in log10-space.
     *
     * @param contextWindow the context for the given locus
     * @return a ten-element array of log-likelihood result of the feature applied to each genotype
     */
    public void compute(VariantContextWindow contextWindow) {
        VariantContext context = contextWindow.getContext();
        likelihoods = new double[10];

        ReadBackedPileup pileup = new ReadBackedPileup(context.getReferenceContext().getBase(), context.getAlignmentContext());
        String primaryBases = pileup.getBases();
        String secondaryBases = pileup.getSecondaryBasePileup();

        for (int genotypeIndex = 0; genotypeIndex < Genotype.values().length; genotypeIndex++) {
            char firstAllele = Genotype.values()[genotypeIndex].toString().charAt(0);
            char secondAllele = Genotype.values()[genotypeIndex].toString().charAt(1);

            int offIsGenotypic = 0;
            int offTotal = 0;

            int onIsGenotypic = 0;
            int onTotal = 0;

            for (int pileupIndex = 0; pileupIndex < primaryBases.length(); pileupIndex++) {
                char primaryBase = primaryBases.charAt(pileupIndex);

                if (secondaryBases != null) {
                    char secondaryBase = secondaryBases.charAt(pileupIndex);

                    if (primaryBase != firstAllele && primaryBase != secondAllele) {
                        if (secondaryBase == firstAllele || secondaryBase == secondAllele) {
                            offIsGenotypic++;
                        }
                        offTotal++;
                    } else {
                        if (secondaryBase == firstAllele || secondaryBase == secondAllele) {
                            onIsGenotypic++;
                        }
                        onTotal++;
                    }
                }
            }

            //System.out.printf(Genotype.values()[genotypeIndex].toString() + " on:\n");
            double offPrior = scaleObservation(offIsGenotypic, offTotal, p2off[genotypeIndex]);

            //System.out.printf(Genotype.values()[genotypeIndex].toString() + " off:\n");
            double onPrior = scaleObservation(onIsGenotypic, onTotal, p2on[genotypeIndex]);

            //System.out.printf("%s: off: %d/%d %f %f%n", Genotype.values()[genotypeIndex].toString(), offIsGenotypic, offTotal, p2off[genotypeIndex], offPrior);
            //System.out.printf("%s: on: %d/%d %f %f%n", Genotype.values()[genotypeIndex].toString(), onIsGenotypic, onTotal, p2on[genotypeIndex], onPrior);

            double logOffPrior = MathUtils.compareDoubles(offPrior, 0.0, 1e-10) == 0 ? Math.log10(Double.MIN_VALUE) : Math.log10(offPrior);
            double logOnPrior = MathUtils.compareDoubles(onPrior, 0.0, 1e-10) == 0 ? Math.log10(Double.MIN_VALUE) : Math.log10(onPrior);

            likelihoods[genotypeIndex] = logOffPrior + logOnPrior;
        }
    }

    public double[] getLikelihoods() {
        return likelihoods;
    }

    public String getStudyHeader() {
        return "";
    }

    public String getStudyInfo() {
        return "";
    }
}
