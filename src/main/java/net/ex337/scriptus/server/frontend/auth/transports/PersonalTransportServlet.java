package net.ex337.scriptus.server.frontend.auth.transports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ex337.scriptus.datastore.impl.jpa.dao.PersonalTransportMessageDAO;
import net.ex337.scriptus.model.api.Message;
import net.ex337.scriptus.server.frontend.auth.BaseServlet;

public class PersonalTransportServlet extends BaseServlet {

    private static final long serialVersionUID = -5504664627735903687L;

    @Override
    protected void doAuthGet(HttpServletRequest req, HttpServletResponse resp, String openid) throws ServletException,
            IOException {
        
        List<PersonalTransportMessageDAO> messages = d.getPersonalTransportMessages(openid);
        
        req.setAttribute("messages", messages);
        
        req.getRequestDispatcher("/WEB-INF/jsp/personal.jsp").forward(req, resp);
        
    }

    @Override
    protected void doAuthPost(HttpServletRequest req, HttpServletResponse resp, String openid) throws ServletException,
            IOException {

        String op = req.getParameter("op");
        
        if("msg".equalsIgnoreCase(op)) {
            
            PersonalTransportMessageDAO m = new PersonalTransportMessageDAO();
            m.message = req.getParameter("msg");;
            m.from = req.getParameter("from");
            m.parent = req.getParameter("parent");
            
            UUID u = d.savePersonalTransportMessage(m);

            final Message mm = new Message(m.from, m.message, System.currentTimeMillis(), openid);
            if(m.parent != null) {
                //FIXME transport!
                mm.setInReplyToMessageId(u.toString());
            }
            
            r.handleIncomings(new ArrayList<Message>() {{add(mm);}});
            
        } else if("del".equalsIgnoreCase(op)) {

            d.deletePersonalTransportMessage(req.getParameter("id"));
            
        }

        req.getRequestDispatcher("/WEB-INF/jsp/personal.jsp").forward(req, resp);

    }

    @Override
    protected String getPageLabel() {
        // TODO Auto-generated method stub
        return null;
    }

}
