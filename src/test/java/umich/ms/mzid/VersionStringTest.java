package umich.ms.mzid;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Avtonomov
 */
public class VersionStringTest {

  @Test
  public void VerifyVersionStringUpdatedBeforeRelease() throws Exception {

    System.out.println("Verifying version information");
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model pom = null;
    if (Files.exists(Paths.get("pom.xml"))) {
      pom = reader.read(new FileReader("pom.xml"));
    }
    if (pom == null) {
      System.err.println("Could not load pom.xml as Model");
      return;
    }

    final String verPom = pom.getVersion();
    final String verCode = Version.version;

    System.out.printf("%nInfo in pom.xml:%n");
    System.out.printf("\tGroup Id: %s%n", pom.getGroupId());
    System.out.printf("\tArtifact Id: %s%n", pom.getArtifactId());
    System.out.printf("\tVersion: %s%n", verPom);
    System.out.printf("%nInfo in Version.java:%n");
    System.out.printf("\tVersion: %s%n", verCode);

    Assert.assertEquals("Version info in pom.xml does not match version info " +
        "returned by Version.version", verPom, verCode);
  }
}
