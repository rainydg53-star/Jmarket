package com.jmarket.config;

import com.jmarket.auth.security.CustomUserDetails;
import com.jmarket.auth.security.CustomUserDetailsService;
import com.jmarket.auth.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new MessagingException("Missing Authorization header");
            }
            String token = authorization.substring("Bearer ".length());
            if (!jwtTokenProvider.validate(token)) {
                throw new MessagingException("Invalid token");
            }

            String email = jwtTokenProvider.parseClaims(token).get("email", String.class);
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            accessor.setUser(authentication);
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new MessagingException("Unauthorized stomp access");
            }
        }
        return message;
    }
}
