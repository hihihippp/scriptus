package net.ex337.scriptus.server.frontend.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ex337.scriptus.config.ScriptusConfig;
import net.ex337.scriptus.datastore.ScriptusDatastore;
import net.ex337.scriptus.scheduler.ProcessScheduler;
import net.ex337.scriptus.transport.MessageRouting;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * 
 * Servlet responsible for CRUDL admin tasks on scripts.
 * 
 * Uses openID for identifying users.
 * 
 * @author ian
 *
 */
public abstract class BaseServlet extends HttpServlet {

	private static final long serialVersionUID = 50869801033071491L;

	protected XmlWebApplicationContext ctx;

	protected ScriptusConfig f;
    
	protected ScriptusDatastore d;

	protected ProcessScheduler s;
	
	protected MessageRouting r;
	
	@Override
	public void init() {

	    ctx = (XmlWebApplicationContext) WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        f = (ScriptusConfig) ctx.getBean("config");
        d = (ScriptusDatastore) ctx.getBean("datastore");
        s = (ProcessScheduler) ctx.getBean("scheduler");
        r = (MessageRouting)ctx.getBean("routing");

	}
	
	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		if(req.getSession(false) == null) {
			resp.sendRedirect(req.getContextPath()+"/");
			return;
		}
		if(req.getSession().getAttribute("openid") == null) {
			resp.sendRedirect(req.getContextPath()+"/");
			return;
		}
		
		String openid=(String) req.getSession().getAttribute("openid");

	      req.setAttribute("pageLabel", getPageLabel());


		doAuthGet(req, resp, openid);
		
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		if(req.getSession(false) == null) {
			resp.sendRedirect(req.getContextPath()+"/");
			return;
		}
		if(req.getSession().getAttribute("openid") == null) {
			resp.sendRedirect(req.getContextPath()+"/");
			return;
		}
		
		String openid=(String) req.getSession().getAttribute("openid");
		
		req.setAttribute("pageLabel", getPageLabel());
		
		doAuthPost(req, resp, openid);

	}

    protected abstract void doAuthGet(HttpServletRequest req, HttpServletResponse resp, String openid) throws ServletException, IOException;
    protected abstract void doAuthPost(HttpServletRequest req, HttpServletResponse resp, String openid) throws ServletException, IOException;
    protected abstract String getPageLabel();

}
