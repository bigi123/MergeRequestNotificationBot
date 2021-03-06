package com.tlc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.leankit.RssWatcher;
import com.tlc.reviewboard.*;
import org.openactive.gitlab.webhook.domain.GitlabEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.Filter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

@Controller
@EnableAutoConfiguration
@Configuration
@ComponentScan("com.tlc")
@EnableAsync
@EnableScheduling
public class Main
{
   @Autowired
   @Qualifier("merge")
   private EventHandler mergeRequestEventHandler;

   @Autowired
   @Qualifier("pipeline")
   private EventHandler pipelineEventHandler;

   @Autowired
   @Qualifier("comment")
   private EventHandler commentEventHandler;

   @Autowired
   private ReviewRequestHandler reviewRequestHandler;

   @Autowired
   private ReviewCommentHandler reviewCommentHandler;

   @Autowired
   private RssWatcher rssWatcher;

   @Bean
   public Filter logFilter()
   {
      GitlabRawEventLogger filter = new GitlabRawEventLogger();
      filter.setIncludePayload(true);
      filter.setMaxPayloadLength(5120);
      return filter;
   }

   @RequestMapping("/")
   @ResponseBody
   public String index( @RequestBody GitlabEvent event )
   {
      if( "merge_request".equalsIgnoreCase( event.getObjectKind() ) )
      {
         mergeRequestEventHandler.handle( event );
      }
      else if( "pipeline".equalsIgnoreCase( event.getObjectKind() ) )
      {
         pipelineEventHandler.handle( event );
      }
      else if( "note".equalsIgnoreCase( event.getObjectKind() ) )
      {
         commentEventHandler.handle( event );
      }
      return "ok";
   }

   @RequestMapping("/rss")
   @ResponseBody
   public String rss()
   {
      rssWatcher.parseAllFeeds();
      return "ok";
   }

   @RequestMapping("/review")
   @ResponseBody
   public String review( @RequestBody ReviewRequestWrapper reviewRequestWrapper )
   {
      try
      {
         reviewRequestHandler.handle( reviewRequestWrapper );
      }
      catch ( Exception e )
      {
         e.printStackTrace();
      }

      return "ok";
   }

   @RequestMapping("/reviewComment")
   @ResponseBody
   public String reviewComment( @RequestBody String data )
   {
      try
      {
         reviewCommentHandler.handle( data );
      }
      catch ( Exception e )
      {
         e.printStackTrace();
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

   @Bean(name = "threadPoolTaskExecutor")
   public Executor threadPoolTaskExecutor()
   {
      return new ThreadPoolTaskExecutor();
   }


   public static void main( String[] args )
   {
      SpringApplication.run( Main.class, args );
   }
}
