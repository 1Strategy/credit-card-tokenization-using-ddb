//   Copyright 2018 1Strategy, LLC

//     Licensed under the Apache License, Version 2.0 (the "License");
//     you may not use this file except in compliance with the License.
//     You may obtain a copy of the License at

//         http://www.apache.org/licenses/LICENSE-2.0

//     Unless required by applicable law or agreed to in writing, software
//     distributed under the License is distributed on an "AS IS" BASIS,
//     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//     See the License for the specific language governing permissions and
//     limitations under the License.

package billingweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.*;
import org.springframework.web.servlet.view.RedirectView;
import billingweb.model.Order;
import java.util.*;

@Controller
public class OrderController {

    public static final String DAL_URL = "Change this to your billing DAL URL.";

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "myorder";
    }

    @RequestMapping("/myorders")
    public String myorders(Model model) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<Order>> rateResponse =
        restTemplate.exchange(DAL_URL + "orders",
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<Order>>() {
            });
        List<Order> orders = rateResponse.getBody();

        model.addAttribute("orders", orders);
        return "myorders";
    }

    @GetMapping("/neworder")
    public String neworder(Model model) {
        model.addAttribute("order", new Order());
        return "neworder";
    }

    @PostMapping("/neworder")
    public RedirectView neworder(@ModelAttribute Order order, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("name", order.getName());
        map.add("detail", order.getDetail());
        map.add("paymentMethod", order.getPaymentMethod());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(
            DAL_URL + "order", request, String.class);
        if (response.getStatusCode() == HttpStatus.CREATED){
            return new RedirectView("myorders");
        } else {
            return new RedirectView("error");
        }
    }
}