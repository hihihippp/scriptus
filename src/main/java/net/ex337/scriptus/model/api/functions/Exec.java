package net.ex337.scriptus.model.api.functions;

import java.io.Serializable;

import net.ex337.scriptus.ProcessScheduler;
import net.ex337.scriptus.datastore.ScriptusDatastore;
import net.ex337.scriptus.model.ScriptAction;
import net.ex337.scriptus.model.ScriptProcess;
import net.ex337.scriptus.transport.Transport;

/**
 * Executes a new script belonging to the same openId user
 * with the arguments provided and the same owner as the
 * script calling exec(). The new process replaces the old
 * process and has the same pid.
 * 
 * @author ian
 *
 */
public class Exec extends ScriptAction implements Serializable {

	private static final long serialVersionUID = -4327593497623694282L;
	
	private String script;
	private String args;
	
	public Exec(String script, String args) {
		super();
		this.script = script;
		this.args = args;
	}



	@Override
	public void visit(ProcessScheduler scheduler, Transport transport, ScriptusDatastore datastore, ScriptProcess process) {

		//TODO add script not found exception
		ScriptProcess p = datastore.newProcess(process.getUserId(), script, args, process.getOwner());
		
		p.setPid(process.getPid());
		
		p.save();
		
		scheduler.execute(process.getPid());

	}

	public String getScript() {
		return script;
	}




	public String getArgs() {
		return args;
	}


}
