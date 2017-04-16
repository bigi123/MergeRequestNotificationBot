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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@EnableAutoConfiguration
@ComponentScan
@Configuration
public class Main
{

   @Autowired
   private SlackBot bot;

   @Autowired
   private JSONConfig config;

   @RequestMapping("/")
   @ResponseBody
   public String index( @RequestBody GitlabEvent event )
   {
      if( "merge_request".equalsIgnoreCase( event.getObjectKind() ) )
      {
         mergeRequestEvent( event );
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

   private void mergeRequestEvent( GitlabEvent event )
   {
      String projectName = event.getAttributes().getTarget().getName();
      String userName = event.getUser().getName();
      String sourceBranchName = event.getAttributes().getSourceBranch();
      String targetBranchName = event.getAttributes().getTargetBranch();
      String state = event.getAttributes().getState();
      String msg = "";
      System.out.println( state );

      List<String> channelNames = channels( sourceBranchName, targetBranchName, projectName );
      if( channelNames.isEmpty() ) return;

      if( "opened".equalsIgnoreCase( state ) || "reopened".equalsIgnoreCase( state ) )
      {
         msg = String.format( "<!here> :mr: %s: *Merge request* from %s : %s → %s", projectName, userName, sourceBranchName, targetBranchName );
         msg += "\n" + event.getAttributes().getUrl();
      }
      else if( "closed".equalsIgnoreCase( state ) )
      {
         msg = String.format( "%s: *Merge request* from %s *closed*", projectName, userName );
      }
      else if( "merged".equalsIgnoreCase( state ) )
      {
         msg = String.format( ":tips: %s: *Merge request* from %s *accepted*", projectName, userName );
      }

      if( !msg.trim().isEmpty() )
      {
         for( String channelName : channelNames )
         {
            bot.say( channelName, msg );
         }
      }
   }

   private List<String> channels( String source, String target, String project )
   {
      return
        config.getChannelToBranchMap().keySet().stream()
        .filter( channelName -> {

           List<String> branches = config.getChannelToBranchMap().get( channelName );
           return branches.stream().anyMatch( branch ->
               branch.toLowerCase().contains( source ) ||
               branch.toLowerCase().contains( target )
           );

        } )
        .collect( Collectors.toList() );
   }

   public static void main( String[] args )
   {
      SpringApplication.run( Main.class, args );
   }
}