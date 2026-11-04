/* VoyaGo - comportements d'interface (sans dépendance externe). */
(function () {
  "use strict";

  function money(n) {
    return n.toLocaleString("fr-FR").replace(/ | /g, " ") + " FCFA";
  }

  // Menu mobile
  document.querySelectorAll("[data-nav-toggle]").forEach(function (btn) {
    var nav = document.getElementById(btn.getAttribute("aria-controls"));
    btn.addEventListener("click", function () {
      var open = nav.classList.toggle("open");
      btn.setAttribute("aria-expanded", open ? "true" : "false");
    });
  });

  // Filtres : panneau replié sur mobile, ouvert sur grand écran
  document.querySelectorAll("details[data-open-desktop]").forEach(function (d) {
    if (window.matchMedia("(min-width: 1024px)").matches) { d.open = true; }
  });

  // Inverser départ / arrivée
  document.querySelectorAll("[data-swap]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      var form = btn.closest("form");
      var a = form.querySelector("[name=from]"), b = form.querySelector("[name=to]");
      var tmp = a.value; a.value = b.value; b.value = tmp;
    });
  });

  // Onglets accessibles
  document.querySelectorAll("[role=tablist]").forEach(function (list) {
    var tabs = list.querySelectorAll("[role=tab]");
    tabs.forEach(function (tab) {
      tab.addEventListener("click", function () {
        tabs.forEach(function (t) {
          var selected = t === tab;
          t.setAttribute("aria-selected", selected ? "true" : "false");
          document.getElementById(t.getAttribute("aria-controls")).hidden = !selected;
        });
      });
    });
  });

  // Compte à rebours du verrou des sièges (RG2)
  document.querySelectorAll("[data-countdown]").forEach(function (el) {
    var remaining = parseInt(el.getAttribute("data-countdown"), 10);
    var label = el.querySelector("[data-countdown-label]") || el;
    function tick() {
      if (remaining <= 0) {
        label.textContent = "Délai expiré : sièges libérés";
        el.classList.add("expired");
        document.querySelectorAll("[data-disable-on-expire]").forEach(function (b) { b.disabled = true; b.classList.add("disabled"); });
        return;
      }
      var m = Math.floor(remaining / 60), s = remaining % 60;
      label.textContent = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
      remaining--;
      setTimeout(tick, 1000);
    }
    tick();
  });

  // Plan de sièges : un formulaire passager par siège sélectionné (F08, F09)
  var bookingForm = document.getElementById("booking-form");
  if (bookingForm) {
    var price = parseInt(bookingForm.getAttribute("data-price"), 10);
    var max = parseInt(bookingForm.getAttribute("data-max"), 10);
    var container = document.getElementById("passengers");
    var template = document.getElementById("passenger-template");
    var saved = {};

    function remember() {
      container.querySelectorAll(".passenger-card").forEach(function (card) {
        var seat = card.getAttribute("data-seat");
        saved[seat] = {
          firstName: card.querySelector("[data-f=firstName]").value,
          lastName: card.querySelector("[data-f=lastName]").value,
          idNumber: card.querySelector("[data-f=idNumber]").value
        };
      });
    }

    function render() {
      remember();
      var seats = Array.prototype.slice.call(bookingForm.querySelectorAll(".seat input:checked"))
        .map(function (i) { return parseInt(i.value, 10); })
        .sort(function (a, b) { return a - b; });
      container.innerHTML = "";
      seats.forEach(function (seat, idx) {
        var node = template.content.firstElementChild.cloneNode(true);
        node.setAttribute("data-seat", seat);
        node.querySelector("[data-seat-label]").textContent = seat;
        node.querySelector("[data-index]").textContent = idx + 1;
        var hidden = node.querySelector("[data-f=seatNumber]");
        hidden.name = "passengers[" + idx + "].seatNumber";
        hidden.value = seat;
        ["firstName", "lastName", "idNumber"].forEach(function (f) {
          var input = node.querySelector("[data-f=" + f + "]");
          input.name = "passengers[" + idx + "]." + f;
          input.id = f + "-" + seat;
          node.querySelector("label[for-f=" + f + "]").setAttribute("for", input.id);
          if (saved[seat]) { input.value = saved[seat][f]; }
        });
        container.appendChild(node);
      });
      var count = seats.length;
      document.querySelectorAll("[data-seat-count]").forEach(function (e) { e.textContent = count; });
      document.querySelectorAll("[data-seat-list]").forEach(function (e) { e.textContent = count ? seats.join(", ") : "aucun"; });
      document.querySelectorAll("[data-total]").forEach(function (e) { e.textContent = money(count * price); });
      document.querySelectorAll("[data-submit]").forEach(function (b) { b.disabled = count === 0; b.classList.toggle("disabled", count === 0); });
      document.getElementById("passengers-empty").hidden = count > 0;
      bookingForm.querySelectorAll(".seat input:not(:checked)").forEach(function (i) {
        if (!i.hasAttribute("data-occupied")) { i.disabled = count >= max; }
      });
    }

    bookingForm.querySelectorAll(".seat input").forEach(function (i) { i.addEventListener("change", render); });
    render();
  }

  // Comptes de démonstration sur la page de connexion
  document.querySelectorAll("[data-demo-email]").forEach(function (btn) {
    btn.addEventListener("click", function () {
      document.getElementById("email").value = btn.getAttribute("data-demo-email");
      document.getElementById("password").value = btn.getAttribute("data-demo-password");
      document.getElementById("password").focus();
    });
  });

  // Confirmation avant les actions sensibles
  document.querySelectorAll("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      if (!window.confirm(form.getAttribute("data-confirm"))) { e.preventDefault(); }
    });
  });
})();
