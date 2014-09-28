package org.flywaydb.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.util.Location;

import static java.nio.file.StandardOpenOption.*;

/**
 * Flyway Rollup Extension.
 */
public class FlywayRollupExtension extends Flyway {
    private String[] developmentLocations;
    private String[] stableLocations;
    private String rollupFileName = "rollup";

    /**
     * remove clean development record in schema_table
     *
     * @throws FlywayException
     */
    public void cleanDevelopment() throws FlywayException {
        if (developmentLocations == null || developmentLocations.length == 0) {
            return;
        }

        List<Path> developmentDir = new ArrayList<>();
        for (String location : developmentLocations) {
            developmentDir.add(Paths.get(new Location(location).getPath()));
        }

        execute((connectionMetaDataTable, connectionUserObjects, dbSupport, schemas) -> {
            List<String> deleteScriptNames = new ArrayList<>();
            developmentDir.stream().forEach(dir -> {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
                    for (Path path : directoryStream) {
                        deleteScriptNames.add(path.getFileName().toString());
                    }
                } catch (IOException e) {
                    throw new FlywayException("fetch dir", e);
                }
            });
            if (!deleteScriptNames.isEmpty()) {
                String[] array = new String[deleteScriptNames.size()];
                for (int i = 0; i < deleteScriptNames.size(); i++) {
                    array[i] = "?";
                }
                try {
                    dbSupport.getJdbcTemplate().execute(
                        "delete from schema_version where script in (" + String.join(",", array) + ")",
                        deleteScriptNames.toArray(new String[deleteScriptNames.size()])
                    );
                } catch (SQLException e) {
                    throw new FlywayException("can not delete recode in schema_version table", e);
                }
            }
            return null;
        });
    }

    /**
     * rollup.
     *
     * @throws FlywayException
     */
    public void rollup() throws FlywayException {
        if (stableLocations == null || stableLocations.length == 0) {
            return;
        }
        execute((connectionMetaDataTable, connectionUserObjects, dbSupport, schemas) -> {
                List<Map<String, String>> schemaVersions = new ArrayList<>();
                try {
                    // データ取得
                    // 中身のデータを取得する
                    schemaVersions = dbSupport.getJdbcTemplate().queryForList("SELECT * FROM " + getTable() + " ORDER BY version_rank")
                        .stream()
                        .filter(map -> map.get("success").equals("1"))
                        .collect(Collectors.toList());

                } catch (SQLException e) {
                    throw new FlywayException("can not fetch schema_version", e);
                }

                if (schemaVersions.isEmpty()) {
                    return null;
                }

                List<Path> stablesDirs = new ArrayList<>();
                for (String location : stableLocations) {
                    stablesDirs.add(Paths.get(new Location(location).getPath()));
                }

                // ディレクトリ毎の処理.
                final List<Map<String, String>> finalSchemaVersions = schemaVersions;
                stablesDirs.stream().forEach(dir -> {
                    Path tmp = Paths.get(dir.getParent().toString(), rollupFileName); // 一時ファイル
                    String lastVersion = null;
                    try (BufferedWriter bw = Files.newBufferedWriter(tmp, CREATE, TRUNCATE_EXISTING);
                        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {

                        // 各ファイルの処理
                        for (Path file : directoryStream) {
                            boolean exists = false;
                            for (Map<String, String> schemaVersion : finalSchemaVersions) {
                                if (schemaVersion.get("script").equals(file.getFileName().toString())) {
                                    lastVersion = schemaVersion.get("version");
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                return;
                            }

                            try (BufferedReader br = Files.newBufferedReader(file)) {
                                br.lines().forEach(str -> {
                                    try {
                                        bw.write(str);
                                        bw.newLine();
                                    } catch (IOException e) {
                                        throw new FlywayException("write error", e);
                                    }
                                });
                            }
                            Files.delete(file);
                        }
                        Path target = Paths.get(dir.toString(), getSqlMigrationPrefix() + lastVersion + "__" + rollupFileName + getSqlMigrationSuffix());
                        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        throw new FlywayException("copy error", e);
                    } finally {
                        if (Files.exists(tmp)) {
                            try {
                                Files.delete(tmp);
                            } catch (IOException e) {
                                // NOP
                            }
                        }
                    }
                });

                return null;
            }

        );
    }

    public String[] getDevelopmentLocations() {
        return developmentLocations;
    }

    public void setDevelopmentLocations(String[] developmentLocations) {
        this.developmentLocations = developmentLocations;
    }

    public String[] getStableLocations() {
        return stableLocations;
    }

    public void setStableLocations(String[] stableLocations) {
        this.stableLocations = stableLocations;
    }

    public String getRollupFileName() {
        return rollupFileName;
    }

    public void setRollupFileName(String rollupFileName) {
        this.rollupFileName = rollupFileName;
    }
}
