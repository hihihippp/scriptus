package net.ex337.scriptus.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.annotation.Resource;

import net.ex337.scriptus.ProcessScheduler;
import net.ex337.scriptus.config.ScriptusConfig;
import net.ex337.scriptus.dao.ScriptusDAO;
import net.ex337.scriptus.exceptions.DAOException;
import net.ex337.scriptus.exceptions.ScriptusRuntimeException;
import net.ex337.scriptus.interaction.InteractionMedium;
import net.ex337.scriptus.model.api.ErrorTermination;
import net.ex337.scriptus.model.api.NormalTermination;
import net.ex337.scriptus.model.support.ContextCall;
import net.ex337.scriptus.model.support.ScriptusClassShutter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.serialize.ScriptableInputStream;
import org.mozilla.javascript.serialize.ScriptableOutputStream;

public class ScriptProcess implements Callable<ScriptAction>, Runnable, Serializable, Cloneable {

	private static final Log LOG = LogFactory.getLog(ScriptProcess.class);

	/**
	 * The length of the ID in bytes.
	 */
	public static final int ID_SIZE_BYTES = 16;

	private static final long serialVersionUID = -7512596370437192858L;

	private UUID pid;
	private UUID waiterPid;
	private String source;
	private String sourceName;
	private String args;
	private String owner;
	private Object state;
	private int version;
	private List<UUID> children = new ArrayList<UUID>();
	private Function compiled;
	private String userId;
	private boolean isRoot;
	
	private transient Object continuation;
	private transient Scriptable globalScope;
	
	@Resource(name="dao")
	private transient ScriptusDAO dao;

	@Resource
	private transient ProcessScheduler scheduler;

	@Resource(name="interaction")
	private transient InteractionMedium interaction;

	@Resource
	private transient ScriptusConfig config;
	
	public ScriptProcess() {
	}

	public void load(final UUID pid) {

//		this.dao = dao;// not set when deserialising below

		if (pid == null) {
			throw new DAOException("Cannot load null pid");
		}

		LOG.debug("loading " + pid.toString().substring(30));

		new ContextCall(config, this, true) {

			private static final long serialVersionUID = 9102537169856280554L;

			@Override
			public void call(Context cx) throws Exception {
				InputStream bais = new ByteArrayInputStream(dao.loadProcess(pid));

				ScriptableInputStream in = new ScriptableInputStream(bais, globalScope);

				ScriptProcess p = (ScriptProcess) in.readObject();
				
				ScriptProcess me = ScriptProcess.this;
				
				me.pid = p.pid;
				me.waiterPid = p.waiterPid;
				me.source = p.source;
				me.sourceName = p.sourceName;
				me.args = p.args;
				me.state = p.state;
				me.children = p.children;
				me.compiled = p.compiled;
				me.userId = p.userId;
				me.owner = p.owner;
				me.isRoot = p.isRoot;
				me.version = p.version;
				
				p = null;

				me.setGlobalScope((ScriptableObject) in.readObject());
				me.setContinuation(in.readObject());

				in.close();
			}
			
		};
		

	}

	public void init(String userId, final String sourceName, String args, String owner) {

		LOG.debug("ctor, source=" + sourceName);

//		this.dao = dao;
		this.userId = userId;
		this.sourceName = sourceName;
		this.args = args;
		this.owner = owner;
		this.source = dao.loadScriptSource(userId, sourceName);
		this.isRoot = true;
		this.version = 0;

		new ContextCall(config, this, true) {

			private static final long serialVersionUID = 7592490313480551981L;

			@Override
			public void call(Context cx) throws Exception {

				compiled = cx.compileFunction(globalScope, "function ___scriptus___ () {"+source+"}", sourceName, 0, null);
			}
		};


	}

	public void save() {
		if (getPid() == null) {
			setPid(UUID.randomUUID());
		} else {
			version++;
		}

		LOG.debug("saving " + getPid().toString().substring(30));

		new ContextCall(config, this, false) {

			private static final long serialVersionUID = 4640396013044598210L;

			@Override
			public void call(Context cx) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				ScriptableOutputStream out = new ScriptableOutputStream(bout, getGlobalScope());
				out.addExcludedName("scriptus");
				out.writeObject(ScriptProcess.this);
				out.writeObject(getGlobalScope());
				out.writeObject(getContinuation());
				out.close();

				dao.writeProcess(getPid(), bout.toByteArray());
				
			}
		};

	}

	public ScriptAction call() {

		Object result;

		try {

			if (continuation == null) {

				LOG.debug("starting new script");

				Context cx = Context.enter();
				cx.putThreadLocal("process", this);
				cx.setClassShutter(new ScriptusClassShutter());

				globalScope.put("args", globalScope, Context.javaToJS(args, globalScope));
				globalScope.put("owner", globalScope, Context.javaToJS(owner, globalScope));

				try {
					// running for first time
					result = cx.callFunctionWithContinuations(compiled, globalScope, new Object[0]);
				} finally {
					Context.exit();

				}

			} else {

				LOG.debug("continuing existing script " + getPid().toString().substring(30));

				if (state instanceof ConvertsToScriptable) {
					state = ((ConvertsToScriptable) state).toScriptable();
				}

				Context cx = Context.enter();
				cx.setClassShutter(new ScriptusClassShutter());
				cx.putThreadLocal("process", this);
				try {
					result = cx.resumeContinuation(continuation, globalScope, state);
				} finally {
					Context.exit();
				}

			}

		} catch (RhinoException e) {

			LOG.error("script error", e);

			return new ErrorTermination(e);

		} catch (ContinuationPending cp) {

			continuation = cp.getContinuation();

			state = cp.getApplicationState();

			LOG.error("script continuation, state obj=" + state.getClass());

			if (state instanceof ScriptAction) {
				return (ScriptAction) state;
			} else {
				throw new ScriptusRuntimeException("Continuation state not ScriptAction:" + state);
			}

		}

		state = result;

		return new NormalTermination(result);

	}


	@Override
	public void run() {
		
		ScriptAction result = this.call();
		
		this.save();

		result.visit(scheduler, interaction, dao, this);
		
	}

	public String getSource() {
		return source;
	}

	public Object getContinuation() {
		return continuation;
	}

	public Object getState() {
		return state;
	}

	public void setState(Object state) {
		this.state = state;
	}

	public List<UUID> getChildren() {
		return children;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	public UUID getWaiterPid() {
		return waiterPid;
	}

	public void setWaiterPid(UUID pid) {
		this.waiterPid = pid;
	}

	public String getArgs() {
		return args;
	}

	public Scriptable getGlobalScope() {
		return globalScope;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setContinuation(Object continuation) {
		this.continuation = continuation;
	}

	public void setChildren(List<UUID> children) {
		this.children = children;
	}

	public void setGlobalScope(Scriptable globalScope) {
		this.globalScope = globalScope;
	}

	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this, o);
	}

	public UUID getPid() {
		return pid;
	}

	public void setPid(UUID pid) {
		this.pid = pid;
	}


	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}


	
	
	public ScriptProcess clone() {
		ScriptProcess r = new ScriptProcess();
		r.args = this.args;
		r.compiled = this.compiled;
		r.continuation = this.continuation;
		r.globalScope = this.globalScope;
		r.source = this.source;
		r.sourceName = this.sourceName;
		r.state = this.state;
		r.userId = this.userId;
		r.dao = this.dao;
		r.scheduler = this.scheduler;
		r.interaction = this.interaction;
		r.config = this.config;
		r.owner = this.owner;
		// ?
		r.isRoot = false;
		r.version = 0;
		r.pid = null;
		r.waiterPid = null;
		r.children = new ArrayList<UUID>();

		return r;

	}

	public void delete() {
		dao.deleteProcess(getPid());
	}

	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

}
