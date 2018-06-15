package com.bitsessential.blog.controllers;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.bitsessential.blog.entities.Comment;
import com.bitsessential.blog.entities.FacebookUser;
import com.bitsessential.blog.entities.Post;
import com.bitsessential.blog.entities.SubComment;
import com.bitsessential.blog.repos.CommentRepository;
import com.bitsessential.blog.repos.PostRepository;
import com.bitsessential.blog.repos.SubCommentRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/posts")
public class PostController {
	private Connection<Facebook> facebookConn;
	private PostRepository postDao;
	private CommentRepository commentDao;
	private SubCommentRepository subCommentDao;
	
	public PostController(Connection<Facebook> conn, PostRepository repo, CommentRepository commentDao, SubCommentRepository subCommentDao) {
        this.facebookConn = conn;
        this.postDao = repo;
        this.commentDao = commentDao;
    	this.subCommentDao = subCommentDao;
    }
	
	@RequestMapping("")
	public String listAllPosts(Model model, @PageableDefault(sort = {"postId"}, direction = Direction.DESC, size = 5) Pageable pageable) {
		Page<Post> posts = postDao.findAll(pageable);
		model.addAttribute("posts", posts);
		return "index";
	}
	
	@RequestMapping(value = "/post", method = RequestMethod.POST)
	public String createPost(Post post) {
		postDao.save(post);
		return "redirect:/posts";
	}

	@RequestMapping(value = "/post/{id}", method = RequestMethod.GET)
	public String getPost(Model model, @PathVariable long id) {
		Optional<Post> post = postDao.findById(id);
		
		List<Comment> comments = commentDao.findAllByPostPostId(id);
		if (comments != null) {
			Collections.sort(comments);
			for(Comment c : comments) {
				List<SubComment> subComments = subCommentDao.findAllByCommentCommentId(c.getCommentId());
				if (subComments != null) {
					Collections.sort(subComments);
					c.setSubComments(subComments);
				}
			}
			
			model.addAttribute("comments", comments);
		}
		
		
		if (post.isPresent()) {
			model.addAttribute("post", post.get());
		}
		
		try {
			FacebookUser user = getFacebookUser();
			model.addAttribute("user", user);
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", null);
	        System.out.println("user null");
		}
		
		return "post";
	}
	
	@RequestMapping(value="/post/{id}", method = RequestMethod.DELETE)
	public String deletePost(Model model, @PathVariable long id) {
		Optional<Post> post = postDao.findById(id);
		if (post.isPresent()) {
			model.addAttribute("post", post.get());
		}
		return "post";
	}
	
	@RequestMapping(value="/post/{id}", method = RequestMethod.PUT)
	public String editPost(Model model, @PathVariable long id) {
		Optional<Post> post = postDao.findById(id);
		if (post.isPresent()) {
			model.addAttribute("post", post.get());
		}
		
		return "post";
	}
	
	private FacebookUser getFacebookUser() {
		try {
			ConnectionData data = facebookConn.createData();
			return new FacebookUser(data.getProviderId(), data.getDisplayName());
		} catch(Exception e) {
			return null;
		}
	}
	
}
