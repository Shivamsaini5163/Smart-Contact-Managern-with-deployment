package com.smartcontactmanager.app.controller;

import com.smartcontactmanager.app.dao.ContactRepository;
import com.smartcontactmanager.app.dao.UserRepository;
import com.smartcontactmanager.app.entities.Contact;
import com.smartcontactmanager.app.entities.User;
import com.smartcontactmanager.app.helper.Message;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    // Define a directory for uploads. This will be created if it doesn't exist.
    private final String UPLOAD_DIR = "uploads/";

    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);
        model.addAttribute("user", user);
    }

    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal) {
        return "normal/user_dashboard";
    }

    @GetMapping("/add-contact")
    public String openAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                 Principal principal, HttpSession session) {
        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);

            if (file.isEmpty()) {
                contact.setImage("no_dp.jpg");
            } else {
                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                contact.setImage(uniqueFileName);

                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path path = uploadPath.resolve(uniqueFileName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            contact.setUser(user);
            user.getContacts().add(contact);
            this.userRepository.save(user);

            session.setAttribute("message", new Message("Your contact is added!! Add more...", "success"));

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", new Message("Something went wrong!!! Try again...", "danger"));
        }
        return "normal/add_contact_form";
    }

    @GetMapping("/show-contacts/{page}")
    public String showContact(@PathVariable("page") Integer page, Model model, Principal principal) {
        model.addAttribute("title", "View Contacts");
        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);
        Pageable pageRequest = PageRequest.of(page, 5);
        Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageRequest);
        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contacts.getTotalPages());
        return "normal/show_contacts";
    }

    @RequestMapping("/contact/{cid}")
    public String showContactDetail(@PathVariable("cid") Integer cid, Model model, Principal principal) {
        Optional<Contact> contactOptional = this.contactRepository.findById(cid);
        if (contactOptional.isPresent()) {
            Contact contact = contactOptional.get();
            String userName = principal.getName();
            User user = this.userRepository.getUserByUserName(userName);
            if (user.getId() == contact.getUser().getId()) {
                model.addAttribute("contact", contact);
                model.addAttribute("title", "Contact- " + contact.getName());
            }
        }
        return "normal/contact_detail";
    }

    @GetMapping("/delete/{cid}")
    public String deleteContact(@PathVariable("cid") Integer cid, Principal principal, HttpSession session) {
        Optional<Contact> contactOptional = this.contactRepository.findById(cid);
        if (contactOptional.isPresent()) {
            Contact contact = contactOptional.get();
            String userName = principal.getName();
            User user = this.userRepository.getUserByUserName(userName);
            if (user.getId() == contact.getUser().getId()) {
                try {
                    if (!contact.getImage().equals("no_dp.jpg")) {
                        Path deletePath = Paths.get(UPLOAD_DIR + contact.getImage());
                        Files.deleteIfExists(deletePath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                user.getContacts().remove(contact);
                this.userRepository.save(user);
                session.setAttribute("message", new Message("Your contact is deleted!!", "success"));
            }
        }
        return "redirect:/user/show-contacts/0";
    }

    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model model) {
        model.addAttribute("title", "Update Contact");
        this.contactRepository.findById(cid).ifPresent(contact -> model.addAttribute("contact", contact));
        return "normal/update_form";
    }

    @PostMapping("/process-update")
    public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                HttpSession session, Principal principal) {
        try {
            Contact oldContactDetails = this.contactRepository.findById(contact.getCid()).get();
            if (!file.isEmpty()) {
                try {
                    if (!oldContactDetails.getImage().equals("no_dp.jpg")) {
                        Path deletePath = Paths.get(UPLOAD_DIR + oldContactDetails.getImage());
                        Files.deleteIfExists(deletePath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                contact.setImage(uniqueFileName);
                Path path = Paths.get(UPLOAD_DIR + uniqueFileName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            } else {
                contact.setImage(oldContactDetails.getImage());
            }
            User user = this.userRepository.getUserByUserName(principal.getName());
            contact.setUser(user);
            this.contactRepository.save(contact);
            session.setAttribute("message", new Message("Your contact is updated!!", "success"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/user/contact/" + contact.getCid();
    }

    @GetMapping("/profile")
    public String yourProfile(Model model) {
        model.addAttribute("title", "Your Profile");
        return "normal/profile";
    }
}