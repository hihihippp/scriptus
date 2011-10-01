package net.ex337.scriptus.dao.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import net.ex337.scriptus.dao.ScriptusDAO;
import net.ex337.scriptus.exceptions.ScriptusRuntimeException;
import net.ex337.scriptus.model.ScriptProcess;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * Base class for all DAO implementations. Contains method implementations
 * common to all subclasses.
 * 
 * @author ian
 *
 */
public abstract class BaseScriptusDAO implements ScriptusDAO {
	
	private static final Log LOG = LogFactory.getLog(BaseScriptusDAO.class);

	/**
	 * to be overridden by Spring to do autowiring
	 */
	public abstract ScriptProcess createScriptProcess();

	@Override
	public final void createTestSources() {
		URL u = this.getClass().getClassLoader().getResource("testScripts");

		if(u == null) {
			return;
		}
		
		File testSources = new File(u.getFile());
		
		if( ! testSources.exists()) {
			return;
		}

		for(File f : testSources.listFiles()) {
			try {
				saveScriptSource("test", f.getName(), FileUtils.readFileToString(f));
			} catch (IOException e) {
				throw new ScriptusRuntimeException(e);
			}
		}
	}

	@Override
	public final ScriptProcess getProcess(UUID pid) {
		
		ScriptProcess result = createScriptProcess();
		
		result.load(pid);
		
		return result;
		
	}



	@Override
	public final ScriptProcess newProcess(String userId, String source, String args, String owner) {

		ScriptProcess result = createScriptProcess();

		result.init(userId, source, args, owner);

		return result;
	}

}
