package cn.edu.bnuz.bell.tm.place.api

import cn.edu.bnuz.bell.workflow.DomainStateMachineHandler
import cn.edu.bnuz.bell.workflow.Events
import cn.edu.bnuz.bell.workflow.IStateObject
import cn.edu.bnuz.bell.workflow.States
import cn.edu.bnuz.bell.workflow.config.DefaultStateMachineConfiguration
import cn.edu.bnuz.bell.workflow.config.DefaultStateMachinePersistConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.persist.StateMachinePersister

@Configuration
@Import([DefaultStateMachineConfiguration, DefaultStateMachinePersistConfiguration])
class WorkflowConfiguration {
    @Bean
    DomainStateMachineHandler domainStateMachineHandler(
            StateMachine<States, Events> stateMachine,
            StateMachinePersister<States, Events, IStateObject> persister) {
        println stateMachine
        new DomainStateMachineHandler(stateMachine, persister)
    }
}
