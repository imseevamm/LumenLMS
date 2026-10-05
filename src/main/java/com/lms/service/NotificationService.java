package com.lms.service;

import com.lms.dao.NotificationDAO;
import com.lms.model.Notification;

import java.util.List;

public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public List<Notification> getForUser(long userId) { return notificationDAO.findByUser(userId); }

    public int unreadCount(long userId) { return notificationDAO.countUnread(userId); }

    public void markAllRead(long userId) { notificationDAO.markAllRead(userId); }

    public void notify(long userId, String title, String message, Notification.Type type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        notificationDAO.insert(n);
    }
}
