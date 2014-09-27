package com.hachiyae.flyway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.flywaydb.core.internal.util.logging.Log;
import org.flywaydb.core.internal.util.logging.LogFactory;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class FlywayTest {
    private static final Log LOG = LogFactory.getLog(FlywayTest.class);

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
    @Option(name = "-X", usage = "-X", aliases = "--debug")
    private boolean debug;

    public static void main(String[] args) {
        System.exit(new FlywayTest().execute(args));
    }

    private int execute(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            if (debug) {
                LOG.info(String.format("arguments is host=%s, port=%s user=%s password=%s database=%s " +
                        "stable-locations=[%s] development-locations=[%s] command=%s debug=%s",
                    host, port, user, password, database,
                    String.join(",", stableLocations), String.join(",", developmentLocations), command, debug));
            }

            Flyway flyway = new Flyway();

            List<String> locations = new ArrayList();
            locations.addAll(Arrays.asList(stableLocations));
            locations.addAll(Arrays.asList(developmentLocations));

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
                LOG.info(System.lineSeparator() + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));
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
            case VALIDATE:
                flyway.validate();
                break;
            default:
                throw new IllegalArgumentException("not found command");
            }
        } catch (Exception e) {
            LOG.error(String.format("An error occurred. %s", e.getMessage()), e);
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

    }
}
