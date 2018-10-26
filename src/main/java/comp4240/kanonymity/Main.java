package comp4240.kanonymity;

import comp4240.kanonymity.incognito.GeneralisationResult;
import comp4240.kanonymity.kanonymity.KAnonymity;
import comp4240.kanonymity.kanonymity.LDiversity;
import lombok.extern.log4j.Log4j2;

import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

@Log4j2
public class Main {

    /** Dataset we're using */
    private Dataset dataset;

    public static void main(String[] args) {
        boolean treeProvided = true;
        Integer k=null,l=null;
        String a="";

        if(args.length == 0)
        {
            System.out.println("You must supply a dataset in order to run the anonymiser");
            return;
        }

        if(args.length == 1)
        {
            treeProvided = false;
            System.out.println("No taxonomy tree was supplied");
        }

        if(args.length > 2)
        {
            a = args[2];
            k = (args.length >= 4) ? Integer.parseInt(args[3]) : null;
            l = (args.length >= 5) ? Integer.parseInt(args[4]) : null;
        }

        try {
            String datasetPath = "data/" + args[0];

            if(treeProvided) {
                String taxonomyPath = "data/" + args[1];
                new Main(datasetPath, taxonomyPath).run(a,k,l);
            }
            else {
                new Main(datasetPath).run(a,k,l);
            }
        } catch (FileNotFoundException e) {
            log.error(e);
            e.printStackTrace();
        }
    }

    private Main(String fileName, String... trees) throws FileNotFoundException {
        if (trees.length > 0) {
            dataset = new Dataset(fileName, trees[0]);
        } else {
            dataset = new Dataset(fileName);
        }
    }

    private void run(String a, Integer k, Integer l) {
        // Desired minimums for algorithms
        Integer desiredK = k;
        Integer desiredL = l;
        boolean kAnonymitySelected = false;
        boolean lDiversitySelected = false;

        // Algorithm flags
        if(a.equalsIgnoreCase("k")){

            if(desiredK==null) {
                System.out.println("k-anonymity has been selected.");
                System.out.println("Please enter your desired value for \"k\":");
                desiredK = getDesiredValue();
            }
            System.out.println("k-anonymity has been selected with value k="+desiredK+".");

            kAnonymitySelected = true;
        } else if (a.equalsIgnoreCase("l")) {

            if(desiredK==null) {
                System.out.println("l-diversity has been selected.");
                System.out.println("Please enter your desired value for \"k\":");
                desiredK = getDesiredValue();
            }else if(desiredL==null) {
                System.out.println("Please enter your desired value for \"l\":");
                desiredL = getDesiredValue();
            }
            System.out.println("l-diversity has been selected with values k="+desiredK+" and l="+desiredL+".");

            lDiversitySelected = true;
        } else {
            Scanner console = new Scanner(System.in);

            System.out.println("Please select the type of algorithm you want to run:");
            System.out.println("\"K\" - k-anonymity; \"L\" - l-diversity");

            while(!kAnonymitySelected && !lDiversitySelected)
            {
                String option = console.nextLine();
                switch(option)
                {
                    case "k":
                    case "K": {
                        System.out.println("k-anonymity has been selected.");

                        System.out.println("Please enter your desired value for \"k\":");
                        desiredK = getDesiredValue();

                        kAnonymitySelected = true;
                        break;
                    }
                    case "l":
                    case "L": {
                        System.out.println("l-diversity has been selected.");

                        System.out.println("Please enter your desired value for \"k\":");
                        desiredK = getDesiredValue();

                        System.out.println("Please enter your desired value for \"l\":");
                        desiredL = getDesiredValue();

                        lDiversitySelected = true;
                        break;
                    }
                    default: {
                        System.out.println("Input not recognised. Please try again:");
                        break;
                    }
                }
            }
        }

        long startTime = System.currentTimeMillis();
        dataset.suppressOutliers();

        System.out.println("The number of combinations for the loaded taxonomy trees is " + dataset.getTaxonomyTreeCombinations() + " combinations");

        dataset.displayDataset(10);

        KAnonymity algorithm = null;

        if(kAnonymitySelected) {
            // Create the K-Anonymity class
            algorithm = new KAnonymity(dataset, desiredK);
        }
        else {
            // Create the L-Diverse class
            algorithm = new LDiversity(dataset, desiredK, desiredL);
        }


        // Anonymise the data set
        GeneralisationResult generalisation = algorithm.anonymise();

        System.out.println(dataset.printEquivalenceClasses());
        algorithm.printStats();

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Elapsed Time: " + (elapsedTime / 1000.0) + " seconds");

        Statistics.getDatasetUtility(generalisation.getNode());

        try {
            createCSV(dataset.modifiedToCSV(),"ModifiedDataset");
            createCSV(dataset.equivalenceToCSV(),"EquivalenceClasses");
        } catch (FileNotFoundException e) {
            System.out.println("Could not save to file. Please ensure file is not open from previous attempts.");
        }
    }

    private int getDesiredValue()
    {
        Scanner console = new Scanner(System.in);
        Integer desiredValue = null;

        while(desiredValue == null)
        {
            try
            {
                desiredValue = (int) Double.parseDouble(console.nextLine());

                if(desiredValue < 1)
                {
                    throw new NumberFormatException();
                }
            }
            catch (NumberFormatException ex) {
                System.out.println("That is not a valid value. Please provide a number >= 1:");
                desiredValue = null;
            }
        }

        return desiredValue;
    }

    private void createCSV(String output, String filename) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new File(filename+".csv"));
        pw.write(output);
        pw.close();
        System.out.println("CSV of "+filename+" can be found at ./KAnonymity/"+filename+".csv");
    }
}