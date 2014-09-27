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

public class FlywayExtend extends Flyway {

    /**
     * rollup.
     *
     * @param stableLocations
     * @param developmentLocations
     * @throws FlywayException
     */
    public void rollup(String[] stableLocations, String[] developmentLocations) throws FlywayException {
        execute((connectionMetaDataTable, connectionUserObjects, dbSupport, schemas) -> {
                List<Map<String, String>> schemaVersions = new ArrayList<>();
                try {
                    // データ取得
                    // 中身のデータを取得する
                    schemaVersions = dbSupport.getJdbcTemplate().queryForList("select * from " + getTable() + " order by version_rank")
                        .stream()
                        .filter(map -> map.get("success").equals("1"))
                        .collect(Collectors.toList());

                } catch (SQLException e) {
                    e.printStackTrace();
                    // TODO:
                }

                if (schemaVersions.isEmpty()) {
                    return null;
                }

                List<Path> developmentDir = new ArrayList<>();
                for (String location : developmentLocations) {
                    developmentDir.add(Paths.get(new Location(location).getPath()));
                }

                List<String> deleteScriptNames = new ArrayList<>();
                developmentDir.stream().forEach(dir -> {
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
                        for (Path path : directoryStream) {
                            deleteScriptNames.add(path.getFileName().toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }

                List<Path> stablesDirs = new ArrayList<>();
                for (String location : stableLocations) {
                    stablesDirs.add(Paths.get(new Location(location).getPath()));
                }

                // ディレクトリ毎の処理.
                final List<Map<String, String>> finalSchemaVersions = schemaVersions;
                stablesDirs.stream().forEach(dir -> {
                    Path tmp = Paths.get(dir.getParent().toString(), "tmp.txt"); // 一時ファイル
                    Path last = null;
                    try (BufferedWriter bw = Files.newBufferedWriter(tmp, CREATE, TRUNCATE_EXISTING);
                        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {

                        // 各ファイルの処理
                        for (Path file : directoryStream) {
                            for (Map<String, String> schemaVersion : finalSchemaVersions) {
                                if (schemaVersion.get("script").equals(file.getFileName().toString())) {
                                    last = file;
                                    break;
                                }
                            }
                            if (last == null) {
                                return;
                            }

                            try (BufferedReader br = Files.newBufferedReader(file)) {
                                br.lines().forEach(str -> {
                                    try {
                                        bw.write(str);
                                        bw.newLine();
                                    } catch (IOException e) {
                                        // TODO: throw exception;
                                    }
                                });
                            }
                            Files.delete(file);
                        }
                        Files.move(tmp, last, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e) {
                        e.printStackTrace();
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

}
