package edu.stanford.bmir.protege.web.importer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

public class ArgsParser {

    public ArgsParser() {
    }

    public static final Set<String> validParts = new LinkedHashSet<>();
    static {
	validParts.add("model");
	validParts.add("hierarchy");
	validParts.add("mappings");
	validParts.add("labels");
	validParts.add("relations");
	validParts.add("relations_co");
	validParts.add("semantic_type");
	//validParts.add("termlists");
    }

    static class ProgramArgs extends LinkedHashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public String getString(String name) {
	    return (String) this.get(name);
	}

	public Boolean getBoolean(String name) {
	    return (Boolean) this.get(name);
	}

	@SuppressWarnings("unchecked")
	public List<String> getStringList(String name) {
	    return (List<String>) this.get(name);
	}
    }

    public ProgramArgs parseCommandLine(String[] args) {
	CommandLine parsedArgs = null;
	Options options = new Options();
	options.addOption("h", "help", false, "show usage");

	// @formatter:off
 	options.addOption(Option.builder("k")
                          .argName("kind")
                          .longOpt("kind")
                          .hasArg()
                          .desc("one of: sample, whole, default=\"sample\"")
                          .build());
 	
 	options.addOption(Option.builder("e")
                 .argName("env")
                 .longOpt("environment")
                 .hasArg()
                 .desc("one of: dev, cert or prod\", default=\"dev\"")
                 .build());
 	
 	options.addOption(Option.builder("u")
                 .argName("uuid")
                 .longOpt("uuid")
                 .hasArg()
                 .desc("UUID of repo (defaulted to current uuid)")
                 .build());
 	
 	options.addOption(Option.builder("userid")
                .argName("userid")
                .longOpt("userid")
                .hasArg()
                .desc("Webprotege userId (defaulted to \"stanAdmin\")")
                .build());
 	
 	options.addOption(Option.builder("projectid")
                .argName("projectid")
                .longOpt("projectid")
                .hasArg()
                .desc("Webprotege projectid (defaulted to \"00000000-0000-0000-0000-000000000000\")")
                .build());
 	
 	options.addOption(Option.builder("phases")
                .argName("phases")
                .longOpt("phases")
                .hasArg()
                .desc(
                   "Import phases to conduct: " +
                   "'E' (\"extract\", i.e. query HGraph), " +
                   "'T' (\"transform\", i.e. model refinement and padding conversion to TTL) " +
                   "'L' (\"load\", i.e. load into WP revisions store.) " +
                   " default: \"ETL\""
                )
                .build());
 	
 	options.addOption(Option.builder("d")
                 .argName("debug")
                 .longOpt("debug")
                 .desc("enable debug logging")
                 .build());
 	
 	options.addOption(Option.builder("p")
                 .argName("parts")
                 .longOpt("parts")
                 .hasArgs()
                 .desc("Graph parts list. e.g. model, hierarchy, semantic_types, relations, labels, mappings and 'all'")
                 .build());
 	
 	options.addOption(Option.builder("input_data_dir")
                 .argName("input_data_dir")
                 .longOpt("input_data_dir")
                 .hasArg()
                 .desc("Top level directory where queries and termlist dirs are under.")
                 .build());
 	
 	options.addOption(Option.builder("output_data_dir")
                 .argName("output_data_dir")
                 .longOpt("output_data_dir")
                 .hasArg()
                 .required()
                 .desc("Top level directory where data-store and lucene-indexes are under.")
                 .build());
 	
 	options.addOption(Option.builder("queries_dir")
                .argName("queries_dir")
                .longOpt("queries_dir")
                .hasArg()
                .desc("Subdirectory containing SPARQL query files. default: \"./queries\"")
                .build());
 	
 	options.addOption(Option.builder("termlists_dir")
                .argName("termlists_dir")
                .longOpt("termlists_dir")
                .hasArg()
                .desc("Subdirectory containing termlists csv files. default: \"./termlists\"")
                .build());
 	// @formatter:on
	CommandLineParser parser = new DefaultParser();
	try {
	    parsedArgs = parser.parse(options, args);
	    if (parsedArgs.hasOption("help"))
		usage(options, null);
	} catch (ParseException e) {
	    usage(options, e);
	}

	// Set arg default values
	ProgramArgs progArgs = new ProgramArgs();
	progArgs.put("debug", false);
	progArgs.put("input_data_dir", "./data");
	progArgs.put("queries_dir", "./queries");
	progArgs.put("termlists_dir", "./termlists");
	progArgs.put("kind", "sample");
	progArgs.put("parts", Arrays.asList(new String[] { "all" }));
	progArgs.put("userid", "stanAdmin");
	progArgs.put("projectid", "00000000-0000-0000-0000-000000000000");
	progArgs.put("phases", "ETL");
	

	Option[] opts = parsedArgs.getOptions();
	for (Option opt : opts) {
	    String arg = opt.getArgName();
	    // out.println(opt);
	    switch (arg) {
	    case ("env"):
		progArgs.put(arg, opt.getValue("dev"));
		break;
	    case ("kind"):
		progArgs.put(arg, opt.getValue("sample"));
		break;
	    case ("parts"):
		for (String oa : opt.getValuesList()) {
		    if (!validParts.contains(oa)) {
			System.err.println("Not a valid part name: " + oa);
			usage(options, null);
		    }
		}
		progArgs.put(arg, opt.getValuesList());
		break;
	    case ("input_data_dir"):
		progArgs.put(arg, opt.getValue("./data"));
		break;
	    case ("output_data_dir"):
		progArgs.put(arg, opt.getValue());
		break;
	    case ("debug"):
		boolean flag = "true".equalsIgnoreCase(opt.getValue()) ? true : false;
		progArgs.put(arg, flag);
		break;
	    case ("uuid"):
		progArgs.put(arg, opt.getValue());
		break;
	    case ("userid"):
		progArgs.put(arg, opt.getValue());
		break;
	    case ("phases"):
		progArgs.put(arg, opt.getValue().toUpperCase());
		break;
	    }
	}

	return progArgs;
    }

    protected void usage(Options options, Exception e) {
	if (e != null)
	    if (e instanceof MissingOptionException || e instanceof UnrecognizedOptionException)
		System.err.println(e.getMessage());
	    else
		e.printStackTrace();
	HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("webprotege-importer", options);
	System.exit(1);
    }

    public static void main(String[] args) throws Exception {
	ArgsParser ap = new ArgsParser();
	ap.parseCommandLine(args);

    }

}
