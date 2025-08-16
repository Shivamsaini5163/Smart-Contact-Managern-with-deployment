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

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;

    // A helper method to get the upload directory path
    private Path getUploadPath() {
        // This will create a directory named 'uploads' in the container's user home directory, which is writable
        String uploadDir = System.getProperty("user.home") + "/uploads";
        Path uploadPath = Paths.get(uploadDir);
        // Create the directory if it doesn't exist
        if (Files.notExists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uploadPath;
    }

    //method for adding common data to response
    @ModelAttribute
    public void addCommonData(Model model, Principal principal){
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        model.addAttribute("user",user);
    }

    // dashboard home
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal) {
        return "normal/user_dashboard";
    }

    //open add form handler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model){
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    //processing add contact form
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                 Principal principal, HttpSession session){
        try{
            String name=principal.getName();
            User user=this.userRepository.getUserByUserName(name);

            if (file.isEmpty()){
                contact.setImage("no_dp.jpg");
            }else{
                contact.setImage(file.getOriginalFilename());
                Path finalPath = getUploadPath().resolve(file.getOriginalFilename());
                Files.copy(file.getInputStream(), finalPath, StandardCopyOption.REPLACE_EXISTING);
            }

            contact.setUser(user);
            user.getContacts().add(contact);
            this.userRepository.save(user);

            session.setAttribute("message", new Message("Your contact is added!! Add more...","success"));
        }catch(Exception e){
            e.printStackTrace();
            session.setAttribute("message", new Message("Something went wrong!!! Try again...","danger"));
        }
        return "normal/add_contact_form";
    }

    //show contacts handler
    @GetMapping("/show-contacts/{page}")
    public String showContact(@PathVariable("page") Integer page, Model model, Principal principal){
        model.addAttribute("title", "View Contacts");
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        Pageable pageRequest=PageRequest.of(page, 5);
        Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(),pageRequest);
        model.addAttribute("contacts",contacts);
        model.addAttribute("currentPage",page);
        model.addAttribute("totalPages",contacts.getTotalPages());
        return "normal/show_contacts";
    }

    //showing particular contact details.
    @RequestMapping("/contact/{cid}")
    public String showContactDetail(@PathVariable("cid") Integer cid, Model model,Principal principal){
        Optional<Contact> contactOptional=this.contactRepository.findById(cid);
        Contact contact=contactOptional.get();
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);
        if(user.getId()==contact.getUser().getId()){
            model.addAttribute("contact",contact);
            model.addAttribute("title", "Contact- "+contact.getName());
        }
        return "normal/contact_detail";
    }

    //delete contact handler
    @GetMapping("/delete/{cid}")
    public String deleteContact(@PathVariable("cid") Integer cid, Model model,Principal principal,HttpSession session){
        Optional<Contact> contactOptional=this.contactRepository.findById(cid);
        Contact contact=contactOptional.get();
        String userName=principal.getName();
        User user=this.userRepository.getUserByUserName(userName);

        if(user.getId()==contact.getUser().getId()) {
            // Delete the image from the filesystem
            try {
                if (!contact.getImage().equals("no_dp.jpg")) {
                    Path deletePath = getUploadPath().resolve(contact.getImage());
                    Files.deleteIfExists(deletePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Remove contact from user's list and delete it
            user.getContacts().remove(contact);
            this.contactRepository.delete(contact);

            session.setAttribute("message", new Message("Your contact is deleted!!","success"));
        }
        return "redirect:/user/show-contacts/0";
    }

    //open update form handler
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model model){
        model.addAttribute("title", "Update Contact");
        Contact contact=this.contactRepository.findById(cid).get();
        model.addAttribute("contact",contact);
        return "normal/update_form";
    }

    //update contact handler
    @PostMapping("/process-update")
    public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,
                                Model model,HttpSession session,Principal principal){
        try{
            Contact oldContactDetails=this.contactRepository.findById(contact.getCid()).get();

            if(!file.isEmpty()){
                // Delete old photo
                if (!oldContactDetails.getImage().equals("no_dp.jpg")) {
                    Path deletePath = getUploadPath().resolve(oldContactDetails.getImage());
                    Files.deleteIfExists(deletePath);
                }
                // Update new photo
                contact.setImage(file.getOriginalFilename());
                Path finalPath = getUploadPath().resolve(file.getOriginalFilename());
                Files.copy(file.getInputStream(), finalPath, StandardCopyOption.REPLACE_EXISTING);
            }else {
                contact.setImage(oldContactDetails.getImage());
            }

            User user=this.userRepository.getUserByUserName(principal.getName());
            contact.setUser(user);
            this.contactRepository.save(contact);
            session.setAttribute("message", new Message("Your contact is updated!!","success"));
        }catch(Exception e){
            e.printStackTrace();
        }
        return "redirect:/user/contact/"+contact.getCid();
    }

    //your profile handler
    @GetMapping("/profile")
    public String yourProfile(Model model){
        model.addAttribute("title", "Your Profile");
        return "normal/profile";
    }
}