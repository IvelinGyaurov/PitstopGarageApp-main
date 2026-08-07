(function () {
    'use strict';

    var modal = document.getElementById('confirm-modal');
    if (!modal) {
        return;
    }

    var titleEl = document.getElementById('confirm-modal-title');
    var messageEl = document.getElementById('confirm-modal-message');
    var acceptBtn = document.getElementById('confirm-modal-accept');
    var pendingForm = null;
    var lastFocus = null;

    function openModal(form) {
        pendingForm = form;
        lastFocus = document.activeElement;

        titleEl.textContent = form.getAttribute('data-confirm-title') || '';
        messageEl.textContent = form.getAttribute('data-confirm-message') || '';
        acceptBtn.textContent = form.getAttribute('data-confirm-action') || 'OK';
        acceptBtn.disabled = false;

        modal.hidden = false;
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('confirm-modal-open');
        acceptBtn.focus();
    }

    function closeModal() {
        modal.hidden = true;
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('confirm-modal-open');
        pendingForm = null;
        acceptBtn.disabled = false;
        if (lastFocus && typeof lastFocus.focus === 'function') {
            lastFocus.focus();
        }
        lastFocus = null;
    }

    function markFormSubmitting(form) {
        var label = form.getAttribute('data-submitting-label');
        if (!label) {
            return;
        }
        var buttons = form.querySelectorAll('button[type="submit"], input[type="submit"]');
        Array.prototype.forEach.call(buttons, function (btn) {
            btn.disabled = true;
            if (btn.tagName === 'BUTTON') {
                btn.textContent = label;
            } else {
                btn.value = label;
            }
        });
    }

    function acceptModal() {
        var form = pendingForm;
        if (!form) {
            closeModal();
            return;
        }
        pendingForm = null;
        acceptBtn.disabled = true;
        modal.hidden = true;
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('confirm-modal-open');
        markFormSubmitting(form);
        HTMLFormElement.prototype.submit.call(form);
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        if (!form.classList.contains('js-confirm-form')) {
            return;
        }
        event.preventDefault();
        openModal(form);
    });

    modal.addEventListener('click', function (event) {
        if (event.target.closest('[data-confirm-dismiss]')) {
            closeModal();
        }
    });

    acceptBtn.addEventListener('click', acceptModal);

    document.addEventListener('keydown', function (event) {
        if (modal.hidden) {
            return;
        }
        if (event.key === 'Escape') {
            event.preventDefault();
            closeModal();
        }
    });
})();
