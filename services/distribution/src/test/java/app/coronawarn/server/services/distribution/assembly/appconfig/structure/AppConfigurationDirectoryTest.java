/*-
 * ---license-start
 * Corona-Warn-App
 * ---
 * Copyright (C) 2020 SAP SE and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package app.coronawarn.server.services.distribution.assembly.appconfig.structure;

import static java.io.File.separator;
import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import app.coronawarn.server.services.distribution.assembly.appconfig.structure.directory.AppConfigurationDirectory;
import app.coronawarn.server.services.distribution.assembly.component.CryptoProvider;
import app.coronawarn.server.services.distribution.assembly.structure.WritableOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.directory.Directory;
import app.coronawarn.server.services.distribution.assembly.structure.directory.DirectoryOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.util.ImmutableStack;
import app.coronawarn.server.services.distribution.common.Helpers;
import app.coronawarn.server.services.distribution.config.DistributionServiceConfig;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@EnableConfigurationProperties(value = DistributionServiceConfig.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CryptoProvider.class}, initializers = ConfigFileApplicationContextInitializer.class)
class AppConfigurationDirectoryTest {

  @Rule
  private TemporaryFolder outputFolder = new TemporaryFolder();

  private final CryptoProvider cryptoProvider;
  private final DistributionServiceConfig distributionServiceConfigSpy;

  @BeforeEach
  void setup() throws IOException {
    reset(distributionServiceConfigSpy);
    outputFolder.create();
  }

  @Autowired
  AppConfigurationDirectoryTest(DistributionServiceConfig distributionServiceConfig, CryptoProvider cryptoProvider) {
    this.distributionServiceConfigSpy = spy(distributionServiceConfig);
    this.cryptoProvider = cryptoProvider;
  }

  @Test
  void createsCorrectFiles() throws IOException {
    Set<String> expFiles = Set.of(
        join(separator, "configuration", "country", "index"),
        join(separator, "configuration", "country", "index.checksum"),
        join(separator, "configuration", "country", "DE", "app_config"),
        join(separator, "configuration", "country", "DE", "app_config.checksum"));

    assertThat(writeDirectoryAndGetFiles()).isEqualTo(expFiles);
  }

  @Test
  void doesNotWriteAppConfigIfValidationFails() throws IOException {
    doReturn("configtests/app-config_mrs_negative.yaml").when(distributionServiceConfigSpy)
        .getAppConfigurationParametersFile();

    Set<String> expFiles = Set.of(
        join(separator, "configuration", "country", "index"),
        join(separator, "configuration", "country", "index.checksum"));

    assertThat(writeDirectoryAndGetFiles()).isEqualTo(expFiles);
  }

  @Test
  void doesNotWriteAppConfigIfUnableToLoadFile() throws IOException {
    doReturn("invalidPath").when(distributionServiceConfigSpy).getAppConfigurationParametersFile();

    Set<String> expFiles = Set.of(
        join(separator, "configuration", "country", "index"),
        join(separator, "configuration", "country", "index.checksum"));

    assertThat(writeDirectoryAndGetFiles()).isEqualTo(expFiles);
  }

  private Set<String> writeDirectoryAndGetFiles() throws IOException {
    File outputFile = outputFolder.newFolder();
    Directory<WritableOnDisk> parentDirectory = new DirectoryOnDisk(outputFile);
    AppConfigurationDirectory configurationDirectory =
        new AppConfigurationDirectory(cryptoProvider, distributionServiceConfigSpy);
    parentDirectory.addWritable(configurationDirectory);

    configurationDirectory.prepare(new ImmutableStack<>());
    configurationDirectory.write();
    return Helpers.getFiles(outputFile, outputFile.getAbsolutePath());
  }
}
