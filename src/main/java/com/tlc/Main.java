package com.tlc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openactive.gitlab.webhook.domain.GitlabEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;

@Controller
@EnableAutoConfiguration
@Configuration
@ComponentScan("com.tlc")
public class Main
{
   @Autowired
   private MergeRequestEventHandler mergeRequestEventHandler;

   @RequestMapping("/")
   @ResponseBody
   public String index( @RequestBody GitlabEvent event )
   {
      if( "merge_request".equalsIgnoreCase( event.getObjectKind() ) )
      {
         mergeRequestEventHandler.mergeRequestEvent( event );
      }
      return "ok";
   }

   @Bean
   public JSONConfig jsonConfig() throws IOException
   {
      String homeDirPath = System.getProperty( "user.home" );
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue( new File( homeDirPath, "SlackBot.json" ), JSONConfig.class );
   }

   public static void main( String[] args )
   {
      SpringApplication.run( Main.class, args );
   }
}
