package com.contactspring;


import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

@RequestMapping("/contacts")
@Controller
public class ContactController {

    private final Logger logger = LoggerFactory.getLogger(ContactController.class);

    private ContactService contactService;

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
    public String update(@Valid Contact contact, BindingResult bindingResult, Model model,
                         HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes,
                         Locale locale, @RequestParam(value="file", required=false) Part file) {

        logger.info("Updating contact");
        if (bindingResult.hasErrors()) {

            model.addAttribute("message", new Message("error",
                    messageSource.getMessage("contact_save_fail", new Object[]{}, locale)));
            model.addAttribute("contact", contact);
            return "contacts/update";
        }
        model.asMap().clear();
        redirectAttributes.addFlashAttribute("message", new Message("success",
                messageSource.getMessage("contact_save_success", new Object[]{}, locale)));

        // Process upload file
        if (file != null) {
            logger.info("File name: " + file.getName());
            logger.info("File size: " + file.getSize());
            logger.info("File content type: " + file.getContentType());
            byte[] fileContent = null;
            try {
                InputStream inputStream = file.getInputStream();
                if (inputStream == null) logger.info("File inputstream is null");
                fileContent = IOUtils.toByteArray(inputStream);
                contact.setPhoto(fileContent);
            } catch (IOException ex) {
                logger.error("Error saving uploaded file");
            }
            contact.setPhoto(fileContent);
        }


        contactService.save(contact);
        return "redirect:/contacts/" + UrlUtil.encodeUrlPathSegment(contact.getId().toString(),
                httpServletRequest);
    }

    @RequestMapping(value = "/{id}", params = "form", method = RequestMethod.GET)
    public String updateForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("contact", contactService.findById(id));
        return "contacts/update";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String create(@Valid Contact contact, BindingResult bindingResult, Model uiModel,
                         HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes,
                         Locale locale, @RequestParam(value="file", required=false) Part file) {
        logger.info("Creating contact");
        if (bindingResult.hasErrors()) {
            uiModel.addAttribute("message", new Message("error",
                    messageSource.getMessage("contact_save_fail", new Object[]{}, locale)));
            uiModel.addAttribute("contact", contact);
            return "contacts/create";
        }
        uiModel.asMap().clear();
        redirectAttributes.addFlashAttribute("message", new Message("success",
                messageSource.getMessage("contact_save_success", new Object[]{}, locale)));

        logger.info("Contact id: " + contact.getId());

        // Process upload file
        if (file != null) {
            logger.info("File name: " + file.getName());
            logger.info("File size: " + file.getSize());
            logger.info("File content type: " + file.getContentType());
            byte[] fileContent = null;
            try {
                InputStream inputStream = file.getInputStream();
                if (inputStream == null) logger.info("File inputstream is null");
                fileContent = IOUtils.toByteArray(inputStream);
                contact.setPhoto(fileContent);
            } catch (IOException ex) {
                logger.error("Error saving uploaded file");
            }
            contact.setPhoto(fileContent);
        }

        contactService.save(contact);
        return "redirect:/contacts/";
    }
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(params = "form", method = RequestMethod.GET)
    public String createForm(Model model) {
        Contact contact = new Contact();
        model.addAttribute(contact);
        return "contacts/create";
    }

    @RequestMapping(value = "/photo/{id}", method = RequestMethod.GET)
    @ResponseBody
    public byte[] downloadPhoto(@PathVariable("id") Long id) {
        Contact contact = contactService.findById(id);

        if (contact.getPhoto() != null) {
            logger.info("Downloading photo for id: {} with size: {}", contact.getId(),
                    contact.getPhoto().length);
        }

        return contact.getPhoto();
    }

    @RequestMapping(value = "/listgrid", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ContactGrid listGrid(@RequestParam(value = "page", required = false) Integer page,
                                @RequestParam(value = "rows", required = false) Integer rows,
                                @RequestParam(value = "sidx", required = false) String sortBy,
                                @RequestParam(value = "sord", required = false) String order) {
        logger.info("Listing contacts for grid with page: {}, rows:{}", page, rows);
        logger.info("Listing contacts for grid with sort: {}, order:{}", sortBy, order);
        //Обработать поле по которому производится сортировка
        Sort sort = null;
        String orderBy = sortBy;
        if (orderBy != null && orderBy.equals("birthDateString")) {
            orderBy="birthDate";
        }
        if (orderBy !=null && order!=null){
            if (order.equals("desc")){
                sort = new Sort(Sort.Direction.DESC , orderBy);
            }else sort = new Sort(Sort.Direction.ASC, orderBy);
        }
        // Сконструировать страничный запрос для текущей страницы
        // Примечание: нумерация страниц для СпрингДатаJPA начинается с 0, а в jqGrid c 1
        PageRequest pageRequest = null;
        if (sort!=null){
            pageRequest = new PageRequest(page - 1, rows, sort);
        } else {
            pageRequest = new PageRequest(page -1, rows);
        }
        Page<Contact> contactPage = contactService.findAllbyPage(pageRequest);
        //сконструировать сетку, которая вернет данные в формате JSON
        ContactGrid contactGrid = new ContactGrid();
        contactGrid.setCurrentPage(contactPage.getNumber()+1);
        contactGrid.setTotalPages(contactPage.getTotalPages());
        contactGrid.setTotalRecords(contactPage.getTotalElements());
        contactGrid.setContactData(Lists.newArrayList(contactPage.iterator()));
        return contactGrid;
    }


    @Autowired
    public void setContactService(ContactService contactService) {
        this.contactService = contactService;
    }

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
}
