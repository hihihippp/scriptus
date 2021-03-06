package net.ex337.scriptus.server.frontend.auth;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ex337.scriptus.config.ScriptusConfig.TransportType;
import net.ex337.scriptus.datastore.ScriptusDatastore;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * Servlet responsible for CRUDL admin tasks on scripts.
 * 
 * Uses openID for identifying users.
 * 
 * @author ian
 *
 */
public class ScriptsServlet extends BaseServlet {

	private static final long serialVersionUID = 50869801033071491L;
	
	@Override
	protected void doAuthGet(HttpServletRequest req, HttpServletResponse resp, String openid) throws ServletException, IOException {
		
		String path = req.getPathInfo();

        req.setAttribute("config", ctx.getBean("config"));

        List<TransportType> transports = d.getInstalledTransports(openid);
        transports.add(TransportType.Personal);
        transports.add(TransportType.Dummy);

        req.setAttribute("transports", transports);

		if("/list/yours".equals(path) || "/list".equals(path)){

		    req.setAttribute("transports", transports);
		    
			Set<String> scripts = d.listScripts(openid);
			
			if(scripts == null || scripts.isEmpty()){
			    
			    req.setAttribute("noscripts", Boolean.TRUE);
			    
	            resp.sendRedirect(req.getContextPath()+"/scripts/list/samples");
	            
			} else {
			    
                req.setAttribute("scripts", scripts);

	            getServletContext().getRequestDispatcher("/WEB-INF/jsp/listScripts.jsp").forward(req, resp);
			}
			
			
			return;

		} else if("/list/samples".equals(path)){

            Set<String> scripts = d.listScripts(ScriptusDatastore.SAMPLE_USER);
            
            req.setAttribute("scripts", scripts);
            req.setAttribute("samples", Boolean.TRUE);

            getServletContext().getRequestDispatcher("/WEB-INF/jsp/listScripts.jsp").forward(req, resp);

            return;

		} else if("/edit".equals(path)) {
			
			String scriptId = req.getParameter("script");
			
			String scriptSource = null;
			
			if(StringUtils.isNotEmpty(scriptId)) {
			    
			    String user = openid;
			    
			    if(Boolean.TRUE.toString().equalsIgnoreCase(req.getParameter("sample"))){
			        user = ScriptusDatastore.SAMPLE_USER;
			        req.setAttribute("sample", Boolean.TRUE);
			    }
				
				scriptSource = d.loadScriptSource(user, scriptId);
				
				if(scriptSource == null) {
					resp.sendError(404);
					return;
				}
			}
			
			req.setAttribute("scriptId", scriptId);
			req.setAttribute("scriptSource", scriptSource);
			
			getServletContext().getRequestDispatcher("/WEB-INF/jsp/editScript.jsp").forward(req, resp);
			
			return;
			
		}

		//if redirect to list, then errors could cause infinite redirects
		resp.sendError(404);
		return;
	}

	//saveScript
	@Override
	protected void doAuthPost(HttpServletRequest req, HttpServletResponse resp, String openid) throws ServletException, IOException {


		String path = req.getPathInfo();

		if("/edit".equals(path)) {
			
			String scriptId = req.getParameter("scriptid");
			String script = req.getParameter("source");
			
			d.saveScriptSource(openid, scriptId, script);
			
			resp.sendRedirect("list");
			return;
			
		} else if("/delete".equals(path)) {
			
			d.deleteScript(openid, req.getParameter("deleteid"));
			resp.sendRedirect("list");
			return;
			
		} else if("/run".equals(path)) {
			
			String script = req.getParameter("runid");
			String args = req.getParameter("args");
			String owner = req.getParameter("owner");
			
			boolean sample = Boolean.TRUE.toString().equalsIgnoreCase(req.getParameter("sample"));
			
			TransportType t = TransportType.valueOf(req.getParameter("transport"));
			
			s.executeNewProcess(openid, script, sample, args, owner, t);

			resp.sendRedirect("list");
			return;

		}

		resp.sendError(404);
	}

    @Override
    protected String getPageLabel() {
        return "scripts";
    }

}
