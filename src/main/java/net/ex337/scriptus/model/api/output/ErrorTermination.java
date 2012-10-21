package net.ex337.scriptus.model.api.output;

import java.io.Serializable;
import java.util.Locale;

import net.ex337.scriptus.model.api.HasStateLabel;
import net.ex337.scriptus.model.api.Termination;

import org.mozilla.javascript.RhinoException;

public class ErrorTermination extends Termination implements Serializable, HasStateLabel {

	private static final long serialVersionUID = -7019866745775324284L;

	private RhinoException error;
	
	public ErrorTermination(RhinoException error) {
		super();
		this.error = error;
	}

	public RhinoException getError() {
		return error;
	}

	@Override
	public Object getResult() {
		
		Throwable t = error;
		
		while(t.getCause() != null) t = t.getCause();
		
		return t.getMessage();
	}

    @Override
    public String getStateLabel(Locale locale) {
        return "Terminated with error "+getResult();
    }
	
}
