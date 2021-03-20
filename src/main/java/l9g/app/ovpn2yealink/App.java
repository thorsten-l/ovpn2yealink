package l9g.app.ovpn2yealink;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Enumeration;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thorsten Ludewig <t.ludewig@ostfalia.de>
 */
public class App
{
  private final static Logger LOGGER = LoggerFactory.getLogger(
    App.class.getName());
  
  private static final int COMMON = 0;
  
  private static final int CA = 1;
  
  private static final int PKCS12 = 2;
  
  private static void writeTarEntry(TarArchiveOutputStream taos, String fileName,
    String fileContent) throws IOException
  {
    TarArchiveEntry tae = new TarArchiveEntry(fileName);
    byte[] content = fileContent.getBytes(Charset.forName("ASCII"));
    tae.setSize(content.length);
    taos.putArchiveEntry(tae);
    taos.write(content);
    taos.closeArchiveEntry();
  }
  
  public static void main(String[] args)
  {
    BuildProperties build = BuildProperties.getInstance();
    LOGGER.info("Project Name    : " + build.getProjectName());
    LOGGER.info("Project Version : " + build.getProjectVersion());
    LOGGER.info("Build Timestamp : " + build.getTimestamp());
    
    try
    {
      Options options = new Options();
      CmdLineParser parser = new CmdLineParser(options);
      
      try
      {
        parser.parseArgument(args);
      }
      catch (CmdLineException e)
      {
        System.out.println(e.getMessage());
        options.setHelp(true);
      }
      
      if (options.isHelp())
      {
        System.out.println("ovpn2yealink usage:");
        parser.printUsage(System.out);
        System.exit(0);
      }
      
      LOGGER.info("Reading OpenVPN config file = {}", options.getInputFilename());

      BufferedReader reader = new BufferedReader(new FileReader(
        options.getInputFilename()));
      
      String line;
      
      int state = COMMON;
      String commonString = "";
      String caString = "";
      String pkcs12String = "";
      
      while ((line = reader.readLine()) != null)
      {
        line = line.trim();
        
        if (line.length() == 0)
        {
          continue;
        }
        
        switch (state)
        {
          case COMMON:
            if ("<ca>".equalsIgnoreCase(line))
            {
              state = CA;
            }
            else if ("<pkcs12>".equalsIgnoreCase(line))
            {
              state = PKCS12;
            }
            else
            {
              commonString += line + '\n';
            }
            break;
          
          case CA:
            if ("</ca>".equalsIgnoreCase(line))
            {
              state = COMMON;
              LOGGER.info("CA certificate length = {}", caString.length());
            }
            else
            {
              caString += line + '\n';
            }
            break;
          
          case PKCS12:
            if ("</pkcs12>".equalsIgnoreCase(line))
            {
              state = COMMON;

              // System.out.println(commonString);
              byte[] pkcs12Bytes = Base64.getDecoder().decode(pkcs12String);
              
              LOGGER.info("PKCS12 bytes length = {}", pkcs12Bytes.length);
              
              KeyStore pkcs12Keystore = KeyStore.getInstance("pkcs12");
              pkcs12Keystore.load(new ByteArrayInputStream(pkcs12Bytes),
                options.getPassword().toCharArray());
              Enumeration<String> aliasEnumeration = pkcs12Keystore.aliases();
              if (aliasEnumeration.hasMoreElements())
              {
                String alias = aliasEnumeration.nextElement();
                LOGGER.info("Keystore entry alias = {}", alias);
                Key key = pkcs12Keystore.getKey(alias, options.getPassword().
                  toCharArray());
                
                Certificate certificate = pkcs12Keystore.getCertificate(alias);
                
                StringWriter pemKeyWriter = new StringWriter();
                JcaPEMWriter writer = new JcaPEMWriter(pemKeyWriter);
                writer.writeObject(key);
                writer.close();

                // System.out.println(pemKeyWriter.toString());
                StringWriter pemCertificateWriter = new StringWriter();
                writer = new JcaPEMWriter(pemCertificateWriter);
                writer.writeObject(certificate);
                writer.close();
                // System.out.println(pemCertificateWriter.toString());

                LOGGER.info("Writing tar file = {}", options.getOuputFilename());
                
                FileOutputStream os = new FileOutputStream(options.
                  getOuputFilename());
                TarArchiveOutputStream taos = new TarArchiveOutputStream(os);
                
                
                TarArchiveEntry tae = new TarArchiveEntry(new Directory("keys"));
                taos.putArchiveEntry(tae);
                taos.closeArchiveEntry();
               
                
                writeTarEntry(taos, "keys/ca.crt", caString);
                writeTarEntry(taos, "keys/client.crt", pemCertificateWriter.
                  toString());
                writeTarEntry(taos, "keys/client.key", pemKeyWriter.toString());

                String configFilename = "config/vpn.cnf";
                
                if ( options.getConfigFilename() != null && options.getConfigFilename().length() > 0 )
                {
                  configFilename = options.getConfigFilename();
                }
                
                LOGGER.debug( "Reading config file = {}", configFilename );
                FileReader fr = new FileReader( configFilename);
                CharBuffer content = CharBuffer.allocate(4096);
                int clen = fr.read(content);
                LOGGER.debug( "Config file length = {}", clen );
                fr.close();
                content.flip();
                writeTarEntry(taos, "vpn.cnf", content.subSequence(0, clen).toString());
                
                taos.close();
              }
            }
            else
            {
              pkcs12String += line;
            }
            break;
        }
      }
      
      reader.close();
    }
    catch (Exception e)
    {
      LOGGER.error("ERROR: {}", e.getMessage());
    }
  }
  
}

class Directory extends File
{
  public Directory(String name)
  {
    super(name);
  }
  
  @Override
  public boolean isDirectory()
  {
    return true;
  }
  
  @Override
  public long lastModified()
  {
    return System.currentTimeMillis();
  }
}
