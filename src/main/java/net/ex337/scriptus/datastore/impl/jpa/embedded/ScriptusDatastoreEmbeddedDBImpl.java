package net.ex337.scriptus.datastore.impl.jpa.embedded;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import net.ex337.scriptus.config.ScriptusConfig;
import net.ex337.scriptus.datastore.impl.jpa.ScriptusDatastoreJPAImpl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ScriptusDatastoreEmbeddedDBImpl extends ScriptusDatastoreJPAImpl {

    private static final Log LOG = LogFactory.getLog(ScriptusDatastoreEmbeddedDBImpl.class);

//    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private String protocol = "jdbc:derby:";

    @Resource
    private ScriptusConfig config;

    @PostConstruct
    public void init() throws SQLException, IOException {

        File configFile = new File(config.getConfigLocation());
        
        //fFIXME configlocation is thee  config file path
        System.setProperty("derby.system.home", configFile.getParent());

        if(config.isCleanInstall()) {

            Connection conn = null;
            Statement s = null;
            
            
            try{
                Properties props = new Properties(); // connection properties
                props.put("user", "scriptus");
                props.put("password", "scriptus");

                String dbName = "scriptus"; // the name of the database
                conn = DriverManager.getConnection(protocol + dbName+ ";create=true", props);
                
                String schema = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("scriptus.derbydb.sql"), "UTF-8");
                
                s = conn.createStatement();
                
                for(String st : StringUtils.split(schema, ";")) {
                    s.execute(st);
                }
                
            } finally {
                
                if(s != null){
                    s.close();
                }
                if(conn != null) {
                    conn.close();
                }
            }



        } else {
            
            //setup normal connection
            
        }


        
    }

    @PreDestroy
    public void destroy() {
        try {
            // the shutdown=true attribute shuts down Derby
            DriverManager.getConnection("jdbc:derby:;shutdown=true");

            // To shut down a specific database only, but keep the
            // engine running (for example for connecting to other
            // databases), specify a database in the connection URL:
            // DriverManager.getConnection("jdbc:derby:" + dbName +
            // ";shutdown=true");
        } catch (SQLException se) {
            if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState())))) {
                // OK
            } else {
                LOG.error("did not shut down normally", se);
            }
        }
    }

}
