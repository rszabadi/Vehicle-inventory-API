package rszabadi.swiss;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

@Controller
public class LoginController {

    @GetMapping(value = "/login", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String loginPage(CsrfToken csrf,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout) {
        String message = "";
        if (error != null) {
            message = "<p class=\"notice error\" role=\"alert\">Invalid username or password.</p>";
        } else if (logout != null) {
            message = "<p class=\"notice\" role=\"status\">You have been logged out.</p>";
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Log in - Vehicle Inventory</title>
                  <link rel="icon" href="/favicon.svg" type="image/svg+xml">
                  <link rel="stylesheet" href="/style.css">
                </head>
                <body class="login-page">
                  <aside class="login-aside">
                    <span class="badge" aria-hidden="true">VI</span>
                    <h1>Vehicle Inventory</h1>
                    <p>Every car, every document, one list.</p>
                  </aside>
                  <main class="login">
                    <form method="post" action="/login">
                      <h2>Log in</h2>
                      %s
                      <label>Username
                        <input name="username" autocomplete="username" required autofocus>
                      </label>
                      <label>Password
                        <input name="password" type="password" autocomplete="current-password" required>
                      </label>
                      <input type="hidden" name="%s" value="%s">
                      <button type="submit">Log in</button>
                    </form>
                  </main>
                </body>
                </html>
                """.formatted(message,
                HtmlUtils.htmlEscape(csrf.getParameterName()),
                HtmlUtils.htmlEscape(csrf.getToken()));
    }
}