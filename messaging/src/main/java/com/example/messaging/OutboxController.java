package com.example.messaging;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@AllArgsConstructor
public class OutboxController {

  private final OutboxService outboxService;

  @GetMapping("/outbox")
  public String outbox(Model model) {
    model.addAttribute("smsList", outboxService.getAllSms());
    model.addAttribute("emails", outboxService.getAllEmails());
    return "outbox";
  }
}
