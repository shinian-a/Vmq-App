package com.shinian.pay;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * 邮件管理类
 */
public class SendmailUtil {

    //以163邮箱为例,需要提前在163邮箱设置里面开启smtp.
    //发送账户
    private static final String SENDER_NAME = "cvbsmds@163.com";
    //发送账户的密码(客户端授权密码)
    private static final String SENDER_PASS = "EPSMTWBUUUHQEUQO";
    //邮箱服务器
    private static final String VALUE_MAIL_HOST = "smtp.163.com";
    //邮箱服务器主机
    private static final String KEY_MAIL_HOST = "mail.smtp.host";
    //邮箱是否需要鉴权
    private static final String KEY_MAIL_AUTH = "mail.smtp.auth";
    //需要鉴权
    private static final String VALUE_MAIL_AUTH = "true";
	
	//收件人名称
	private static String Recipient_Name = "shiniana@qq.com";
	
	
    public static SendmailUtil getInstance() {
        return InstanceHolder.instance;
    }

    private SendmailUtil() {
    }

    private static class InstanceHolder {
        private static SendmailUtil instance = new SendmailUtil();
    }

    class MailTask extends AsyncTask<Void, Void, Boolean> {

        private MimeMessage mimeMessage;

        public MailTask(MimeMessage mimeMessage) {
            this.mimeMessage = mimeMessage;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Transport.send(mimeMessage);
                return Boolean.TRUE;
            } catch (MessagingException e) {
                e.printStackTrace();
                return Boolean.FALSE;
            }
        }
    }
    //发送普通邮件
    public void sendMail(String title,String content) {
        MimeMessage mimeMessage = createMessage(title, content,Recipient_Name);
        MailTask mailTask = new MailTask(mimeMessage);
        mailTask.execute();
    }
    //发送带附件
    public void sendMailWithFile(String title, String content, String filePath) {
        MimeMessage mimeMessage = createMessage(title, content,Recipient_Name);
        appendFile(mimeMessage, filePath);
        MailTask mailTask = new MailTask(mimeMessage);
        mailTask.execute();
    }
    //发送多个附件
    public void sendMailWithMultiFile(String title, String content, List<String> pathList) {
        MimeMessage mimeMessage = createMessage(title, content,Recipient_Name);
        appendMultiFile(mimeMessage, pathList);
        MailTask mailTask = new MailTask(mimeMessage);
        mailTask.execute();
    }

    private Authenticator getAuthenticator() {
        return new Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_NAME, SENDER_PASS);
            }
        };
    }

    private MimeMessage createMessage(String title, String content,String Recipient_Name) {
        Properties properties = System.getProperties();
        properties.put(KEY_MAIL_HOST, VALUE_MAIL_HOST);
        properties.put(KEY_MAIL_AUTH, VALUE_MAIL_AUTH);
        Session session = Session.getInstance(properties, getAuthenticator());
        //创建消息
        MimeMessage mimeMessage = new MimeMessage(session);
        try {
            //设置发送者
            mimeMessage.setFrom(new InternetAddress(SENDER_NAME));
            //设置接收者
            InternetAddress[] addresses = new InternetAddress[]{new InternetAddress(Recipient_Name)};
            mimeMessage.setRecipients(Message.RecipientType.TO,Recipient_Name);
            //设置邮件的主题
            mimeMessage.setSubject(title);
            //设置邮件的内容
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(content, "text/html;charset=gbk");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            mimeMessage.setContent(multipart);
            //设置发送时间
            mimeMessage.setSentDate(new Date());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return mimeMessage;
    }

    //添加文件附件
    private void appendFile(MimeMessage message, String filePath) {
        try {
            Multipart multipart = (Multipart) message.getContent();
            MimeBodyPart filePart = new MimeBodyPart();
            filePart.attachFile(filePath);
            multipart.addBodyPart(filePart);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    //添加多个文件附件
    private void appendMultiFile(MimeMessage message, List<String> pathList) {
        try {
            Multipart multipart = (Multipart) message.getContent();
            for (String path : pathList) {
                MimeBodyPart filePart = new MimeBodyPart();
                filePart.attachFile(path);
                multipart.addBodyPart(filePart);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
