package l9g.app.ovpn2yealink;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

/**
 *
 * @author Thorsten Ludewig <t.ludewig@ostfalia.de>th
 */
public class Options
{
  @Setter
  @Getter
  @Option( name="--help", aliases="-h", usage="Display this help", required = false )
  private boolean help = false;
  
  @Getter
  @Option( name="--password", aliases="-p", usage="Keystore (PKCS12) password", required = true )
  private String password;

  @Getter
  @Option( name="--input", aliases="-i", usage="OpenVPN configuration file", required = true )
  private String inputFilename;

  @Getter
  @Option( name="--output", aliases="-o", usage="Yealink OpenVPN tar file", required = true )
  private String ouputFilename;
  
  @Getter
  @Option( name="--config", aliases="-c", usage="OpenVPN config file", required = false )
  private String configFilename;
}
