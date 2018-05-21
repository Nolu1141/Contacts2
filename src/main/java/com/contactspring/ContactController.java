package com.contactspring;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@RequestMapping("/contacts")
@Controller
public class ContactController {

    private final Logger logger = LoggerFactory.getLogger(ContactController.class);

    private ContactService contactService;
    @Autowired
    private MessageSource messageSource;

    @RequestMapping(method = RequestMethod.GET)
    public String list(Model model) {
        logger.info("Listing contacts");
        List<Contact> contacts = contactService.findAll();
        model.addAttribute("contacts", contacts);
        logger.info("No. of contacts: " + contacts.size());
        return "contacts/list";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String show(@PathVariable("id") Long id, Model model) {
        Contact contact = contactService.findById(id);
        model.addAttribute("contact", contact);
        return "contacts/show";
    }

    @RequestMapping(value = "/{id}", params = "form", method = RequestMethod.POST)
    public String update(Contact contact, BindingResult bindingResult, Model uiModel,
                         HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes,
                         Locale locale) {

        logger.info("Updating contact");
        if (bindingResult.hasErrors()) {

            uiModel.addAttribute("message", new Message("error",
                    messageSource.getMessage("contact_save_fail", new Object[]{}, locale)));
            uiModel.addAttribute("contact", contact);
            return "contacts/update";
        }
        uiModel.asMap().clear();
        redirectAttributes.addFlashAttribute("message", new Message("success",
                messageSource.getMessage("contact_save_success", new Object[]{}, locale)));

        contactService.save(contact);
        return "redirect:/contacts/" + UrlUtil.encodeUrlPathSegment(contact.getId().toString(),
                httpServletRequest);
    }
    @RequestMapping(value = "/{id}", params = "form", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") Long id, Model model){
        model.addAttribute("contact", contactService.findById(id));
        return "contacts/update";
    }

    @Autowired
    public void setContactService(ContactService contactService) {
        this.contactService = contactService;
    }


}
