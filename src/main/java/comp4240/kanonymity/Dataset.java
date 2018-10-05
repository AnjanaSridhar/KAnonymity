// https://en.wikipedia.org/wiki/K-anonymity

package comp4240.kanonymity;

import comp4240.kanonymity.attribute.*;
import comp4240.kanonymity.tree.Range;
import comp4240.kanonymity.tree.Tree;
import comp4240.kanonymity.tree.TreeDefault;
import comp4240.kanonymity.tree.TreeRange;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Dataset {

    private List<String> headers;
    private int[] headerWidths;
    private List<IdentifierType> identifiers;
    private List<AttributeType> attributeTypes;
    private List<Record> records;
    private HashMap<String, Tree> generalisations;

    private List<Record> filtered;

    public Dataset(String fileName, String taxonomyFileName) {
        this(fileName);
        loadTaxonomyTrees(taxonomyFileName);
    }

    public Dataset(String fileName) {
        this.records = new ArrayList<>();
        this.generalisations = new HashMap<>();
        this.filtered = null;
        loadData(fileName);
    }

    public void loadData(String path) {
        //System.out.println("[INFO]   loadData   Loading data");
        Scanner scanner;
        String line;

        try {
            File file = new File(path);
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // Check the files not blank
        if (!scanner.hasNext()) {
            return;
        }

        // Identifier Type
        line = scanner.nextLine();
        setIdentifierType(line.split(","));

        // Set the header widths, used for displaying the dataset
        headerWidths = new int[identifiers.size()];
        setHeaders(line.split(","));

        // Attribute Type
        line = scanner.nextLine();
        setAttributeTypes(line.split(","));
        setHeaders(line.split(","));

        // Headers
        line = scanner.nextLine();
        setHeaders(line.split(","));
        setHeaderWidths(line.split(","));


        // Data
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            addRecord(line.split(","));
            setHeaderWidths(line.split(","));
        }
        scanner.close();
    }

    private void setHeaderWidths(String[] values) {
        for (int i = 0; i < values.length; i++) {
            String value = values[i];

            headerWidths[i] = Math.max(headerWidths[i], value.length());
            headerWidths[i] = Math.max(headerWidths[i], 6);
        }
    }

    public void loadTaxonomyTrees(String path) {
        //System.out.println("[INFO]   loadTaxonomyTrees   Loading TaxonomyTrees");
        Scanner scanner;
        String line;

        try {
            File file = new File(path);
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // Check the files not blank
        if (!scanner.hasNext()) {
            return;
        }

        // Loop through the file line by line
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();

            // Check the line isn't empty
            if (line.trim().isEmpty()) {
                continue;
            }

            // Split the line by commas
            String[] values = line.split(",");

            // Trim all the results in the values
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }

            // Get the header for the taxonomy tree
            String header = values[0];

            // Find out what index the header is in the data set to find the corresponding attributeType for the column.
            int headerIndex = headers.indexOf(header);
            AttributeType attributeType = attributeTypes.get(headerIndex);

            // Depending on the attribute type call each respective function.
            switch (attributeType) {
                case STRING:
                    addTaxonomyTreeNodeString(values);
                    break;
                case NUMERIC:
                    addTaxonomyTreeNodeRange(values);
                    break;
            }
        }
    }

    /**
     * Given an array of values:
     * Index 0:     Header of the data set column used to reference the generalisation tree
     * Index 1:     The parent node
     * Index 2+:    The children nodes that will be added to the parent node
     * @param values
     */
    private void addTaxonomyTreeNodeString(String[] values) {
        // Header of the data set column
        String header = values[0];

        // Get the generalisation tree
        TreeDefault tree = (TreeDefault) generalisations.get(header);

        // If the tree doesn't exist then create it
        if (tree == null) {
            tree = new TreeDefault(header);
            addGeneralisation(tree);
        }

        // Get the parent node
        String parent = values[1];

        // Add all the children to that node
        for (int i = 2; i < values.length; i++) {
            tree.add(parent, values[i]);
        }
    }

    /**
     * Given an array of values:
     * Index 0:     Header of the data set column used to reference the generalisation tree
     * Index 1:     The parent node
     * Index 2+:    The children nodes that will be added to the parent node
     * @param values
     */
    private void addTaxonomyTreeNodeRange(String[] values) {
        // Header of the data set column
        String header = values[0];

        // Get the generalisation tree
        TreeRange tree = (TreeRange) generalisations.get(header);

        // If the tree doesn't exist then create it
        if (tree == null) {
            tree = new TreeRange(header);
            addGeneralisation(tree);
        }

        // Get the parent node
        Range parent = new Range(values[1]);

        // Add all the children to that node
        for (int i = 2; i < values.length; i++) {
            Range child = new Range(values[i]);
            tree.add(parent, child);
        }
    }

    private void setIdentifierType(String[] values) {
        identifiers = new ArrayList<>(values.length);
        for (String value : values) {
            value = value.trim();
            IdentifierType type = IdentifierType.getType(value);
            identifiers.add(type);
        }
    }

    /**
     * Takes an array of Strings and converts the values to an enum value to set the attribute types of each column.
     * @param values the array containing all the attribute types for each column.
     */
    private void setAttributeTypes(String[] values) {
        attributeTypes = new ArrayList<>(values.length);
        for (String value : values) {
            value = value.trim();
            AttributeType type = AttributeType.getType(value);
            attributeTypes.add(type);
        }
    }

    /**
     * Takes an array of Strings and sets the headers to the corresponding values
     * @param values the array containing the header values.
     */
    public void setHeaders(String[] values) {
        headers = new ArrayList<>(values.length);
        for (String value : values) {
            headers.add(value.trim());
        }
    }

    public void addRecord(String[] values) {
        Record record = new Record();
        for (int i = 0; i < values.length; i++) {
            String value = values[i].trim();
            Attribute attribute;

            switch (attributeTypes.get(i)) {
                case STRING:
                    attribute = new StringAttribute(value, identifiers.get(i));
                    break;
                case NUMERIC:
                    Integer v = Integer.parseInt(value);
                    attribute = new NumericAttribute(v, identifiers.get(i));
                    break;
                case BINARY:
                    attribute = new BinaryAttribute(value, identifiers.get(i));
                    break;
                case DATE:
                    attribute = new DateAttribute(value, identifiers.get(i));
                    break;
                default:
                    throw new IllegalArgumentException("addRecord :: The Attribute Type: '" + attributeTypes.get(i) + "' is not recognised.");
            }

            record.addAttribute(attribute);
        }

        records.add(record);
    }

    /**
     * Reset modified values and if the record is suppressed
     */
    public void hardReset() {
        filtered = null;
        for (Record r : records) {
            r.hardReset();
        }
    }

    /**
     * Reset modified values. Not if the record is suppressed
     */
    public void resetModifiedValues() {
        for (Record r : records) {
            r.resetModifiedValues();
        }
    }

    /**
     * Loop through all records and collect the attributes from a header column
     * @param header
     * @return
     */
    public List<Attribute> getAttributes(String header) {
        int headerIndex = headers.indexOf(header);
        return getAttributes(headerIndex);
    }

    public List<Attribute> getAttributes(int column) {
        List<Attribute> attributes = new ArrayList<>();

        for (Record r : records) {
            List<Attribute> recordAttributes = r.getAttributes();
            attributes.add(recordAttributes.get(column));
        }

        return attributes;
    }

    public void displayDataset() {
        displayDataset(records.size());
    }

    public void displayDataset(int amount) {

        System.out.println("Some of the Dataset");
        for (int i = 0; i < headers.size(); i++) {
            AttributeType attributeType = attributeTypes.get(i);
            String format = "%-" + headerWidths[i] + "s ";
            System.out.printf(format, attributeType);
        }
        System.out.println();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String format = "%-" + headerWidths[i] + "s ";
            System.out.printf(format, header);
        }
        System.out.println();

        for (int i = 0; i < Math.min(amount, records.size()); i++) {
            Record r = records.get(i);
            List<Attribute> attributes = r.getAttributes();
            for (int j = 0; j < attributes.size(); j++) {
                Attribute attribute = attributes.get(j);
                String format = "%-" + headerWidths[j] + "s ";
                System.out.printf(format, attribute.toString());
            }
            System.out.println();
        }
    }

    public void displayModifiedDataset() {
        displayModifiedDataset(records.size());
    }

    public void displayModifiedDataset(int amount) {
        System.out.println("\nThe Modified Dataset");
        for (int i = 0; i < headers.size(); i++) {
            AttributeType attributeType = attributeTypes.get(i);
            String format = "%-" + headerWidths[i] + "s ";
            System.out.printf(format, attributeType);
        }
        System.out.println();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String format = "%-" + headerWidths[i] + "s ";
            System.out.printf(format, header);
        }
        System.out.println();

        for (int i = 0; i < Math.min(amount, records.size()); i++) {
            Record r = records.get(i);
            List<Attribute> attributes = r.getAttributes();
            for (int j = 0; j < attributes.size(); j++) {
                Attribute attribute = attributes.get(j);
                String format = "%-" + headerWidths[j] + "s ";
                System.out.printf(format, attribute.getModifiedValue());
            }
            System.out.println();
        }
    }

    public void addGeneralisation(Tree tree) {
        String attributeHeader = tree.getAttributeHeader();
        generalisations.put(attributeHeader, tree);
    }

    public List<Record> getRecords() {
        if (filtered != null) {
            return filtered;
        }

        filtered = new ArrayList<>();
        for (Record r : records) {
            if (!r.isSuppressed()) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public int getSize() {
        return records.size();
    }

    public List<IdentifierType> getIdentifiers() {
        return identifiers;
    }

    public int getAttributeSize() {
        return attributeTypes.size();
    }

    public List<String> getHeaders() {
        return headers;
    }

    public Tree getGeneralisationTree(String key) {
        return generalisations.get(key);
    }

    public List<Tree> getGeneralisations() {
        return new ArrayList<>(generalisations.values());
    }

    public int getTaxonomyTreeCombinations() {
        int combinations = 1;
        for (String header : headers) {
            Tree tree = generalisations.get(header);
            if (tree != null) {
                combinations *= tree.getTreeHeight() + 1;
            }
        }
        return combinations;
    }

    // Debug method
    public void printEquivalenceClasses() {
        HashMap<String, Integer> equivalenceClasses = new HashMap<>();

        for (Record r : getRecords()) {
            String modifiedValues = r.getModifiedQIDValues();
            Integer size = equivalenceClasses.get(modifiedValues);
            size = (size == null) ? 1 : size + 1;
            equivalenceClasses.put(modifiedValues, size);
        }

        System.out.println("\nEquivalence Classes");
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String format = "%-" + headerWidths[i] + "s ";
            System.out.printf(format, header);
        }
        System.out.println();

        Iterator<Map.Entry<String, Integer>> it = equivalenceClasses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> pair = it.next();
            it.remove();
            String value = pair.getKey();
            String[] values = value.split("\t");


            for (int j = 0; j < values.length; j++) {
                String format = "%-" + headerWidths[j] + "s ";
                System.out.printf(format, values[j]);
            }
            System.out.println("\tEquivalence Class Size: " + pair.getValue());
        }
    }

    public List<String> getQIDHeaders() {
        List<String> QIDs = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (identifiers.get(i) == IdentifierType.QID) {
                QIDs.add(headers.get(i));
            }
        }
        return QIDs;
    }

    public HashMap<String, AttributeCount> getAttributeCounts(List<String> headerQIDs) {
        HashMap<String, AttributeCount> counts = new HashMap<>();
        for (Record r : records) {
            List<Attribute> recordQIDs = r.getQIDs();
            for (int i = 0; i < headerQIDs.size(); i++) {
                String qid = headerQIDs.get(i);
                AttributeCount counter = counts.get(qid);
                if (counter == null) {
                    counter = new AttributeCount(qid);
                    counts.put(qid, counter);
                }
                counter.add(recordQIDs.get(i).toString());
            }
        }
        return counts;
    }

    public void suppressOutliers() {
        List<String> qids = getQIDHeaders();
        HashMap<String, AttributeCount> countMap = getAttributeCounts(qids);

        int suppressed = 0;
        for (int i = 0; i < qids.size(); i++) {
            String qid = qids.get(i);
            AttributeCount counts = countMap.get(qid);
            double mean = counts.getMean();
            double stdDev = counts.getStdDev();

            for (Record r : getRecords()) {
                if (r.isSuppressed()) {
                    continue;
                }

                Attribute attribute = r.getQIDs().get(i);
                int count = counts.getCounts().get(attribute.toString());
                if (count < mean - stdDev) {
                    r.setSuppressed();
                    suppressed++;
                }
            }
        }
        this.filtered = null;
        System.out.println("Suppressed " + suppressed + " rows!");
    }
}