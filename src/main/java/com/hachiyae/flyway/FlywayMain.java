package com.hachiyae.flyway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.FlywayRollupExtension;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class FlywayMain {
    @Option(name = "--help", usage = "--help")
    private boolean help;
    @Option(name = "-h", usage = "-h localhost", aliases = "--host")
    private String host = "localhost";
    @Option(name = "-P", usage = "-P 3306", aliases = "--port")
    private String port = "3306";
    @Option(name = "-d", usage = "-d test", aliases = "--database", required = true)
    private String database;
    @Option(name = "-u", usage = "-u user", aliases = "--user", required = true)
    private String user;
    @Option(name = "-p", usage = "-p password", aliases = "--password", required = true)
    private String password;
    @Option(name = "-s", usage = "-s filesystem:db/dir1 filesystem:db/dir2", aliases = "--stable-locations", handler = StringArrayOptionHandler.class)
    private String[] stableLocations;
    @Option(name = "-D", usage = "-D filesystem:db/dir1 filesystem:db/dir2", aliases = "--development-locations", handler = StringArrayOptionHandler.class)
    private String[] developmentLocations;
    @Option(name = "-c", usage = "-c migrate", aliases = "--command")
    private FlywayCommand command = FlywayCommand.INFO;

    /** optional * */
    @Option(name = "--encode", usage = "--encode utf8")
    private String encode = "utf8";
    @Option(name = "-X", usage = "-X")
    private boolean debug;
    @Option(name = "-o", usage = "-o rollup", aliases = "--output-description")
    private String outputDescription = "rollup";

    public static void main(String[] args) {
        System.exit(new FlywayMain().execute(args));
    }

    private int execute(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            if (debug) {
                System.out.printf(String.format("arguments is host=%s, port=%s user=%s password=%s database=%s " +
                        "stable-locations=[%s] development-locations=[%s] command=%s debug=%s\n",
                    host, port, user, password, database,
                    String.join(",", stableLocations), String.join(",", developmentLocations), command, debug));
            }

            FlywayRollupExtension flyway = new FlywayRollupExtension();

            List<String> locations = new ArrayList<>();
            locations.addAll(Arrays.asList(stableLocations));
            locations.addAll(Arrays.asList(developmentLocations));
            flyway.setStableLocations(stableLocations);
            flyway.setDevelopmentLocations(developmentLocations);
            flyway.setOutputDescription(outputDescription);

            flyway.setDataSource(String.format("jdbc:mysql://%s:%s/%s", host, port, database), user, password);
            if (!locations.isEmpty()) {
                flyway.setLocations(locations.toArray(new String[locations.size()]));
            }

            flyway.setEncoding(encode);
            switch (command) {
            case CLEAN:
                flyway.clean();
                break;
            case INFO:
                System.out.println(System.lineSeparator() + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));
                break;
            case INIT:
                flyway.init();
                break;
            case MIGRATE:
                flyway.migrate();
                break;
            case REPAIR:
                flyway.repair();
                break;
            case ROLLUP:
                flyway.rollup();
                break;
            case DEV_CLEAN:
                flyway.cleanDevelopment();
                break;
            case VALIDATE:
                flyway.validate();
                break;
            default:
                throw new IllegalArgumentException("not found command");
            }
        } catch (CmdLineException e) {
            if (help) {
                System.out.printf(
                    "Usage: %n\tjava %s %s%n",
                    getClass().getName(), parser.printExample(OptionHandlerFilter.ALL));
                parser.printUsage(System.out);
                return 0;
            } else {
                System.err.printf(
                    "Usage: %n\tjava %s %s%n",
                    getClass().getName(), parser.printExample(OptionHandlerFilter.ALL));
                parser.printUsage(System.err);
                return 1;
            }
        } catch (Exception e) {
            System.err.printf(String.format("An error occurred. %s\n%s\n", e.getMessage(), e.getStackTrace()[0]));
            return 1;
        }
        return 0;
    }

    private enum FlywayCommand {
        CLEAN,
        INFO,
        INIT,
        MIGRATE,
        REPAIR,
        VALIDATE,
        ROLLUP,
        DEV_CLEAN
    }
}
