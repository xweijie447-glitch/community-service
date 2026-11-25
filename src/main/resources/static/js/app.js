/* ============================================================
   通用交互：Toast、CSRF、确认框、评分控件、表单校验
   ============================================================ */
(function () {
    'use strict';

    var meta = function (name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute('content') : '';
    };

    window.CSRF = {
        token: meta('_csrf'),
        header: meta('_csrf_header') || 'X-XSRF-TOKEN'
    };

    /** 统一 fetch 封装，自动带 CSRF 头 */
    window.postJSON = function (url, body) {
        return fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                [window.CSRF.header]: window.CSRF.token
            },
            body: JSON.stringify(body || {})
        }).then(function (resp) {
            return resp.json().catch(function () {
                return { code: resp.status, message: '服务异常（' + resp.status + '）' };
            });
        });
    };

    /* ---------------- Toast ---------------- */
    function ensureToastWrap() {
        var wrap = document.querySelector('.toast-wrap');
        if (!wrap) {
            wrap = document.createElement('div');
            wrap.className = 'toast-wrap';
            document.body.appendChild(wrap);
        }
        return wrap;
    }

    window.toast = function (message, type, timeout) {
        if (!message) return;
        var wrap = ensureToastWrap();
        var el = document.createElement('div');
        el.className = 'toast ' + (type || 'info');
        var icon = type === 'success' ? '✓' : type === 'error' ? '!' : 'ℹ';
        el.innerHTML = '<span>' + icon + '</span><span>' + message + '</span>';
        wrap.appendChild(el);
        setTimeout(function () {
            el.style.opacity = '0';
            el.style.transform = 'translateY(-6px)';
            el.style.transition = 'all .25s';
            setTimeout(function () { el.remove(); }, 260);
        }, timeout || 3200);
    };

    /** 服务端 flash 消息 -> toast */
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-flash]').forEach(function (el) {
            window.toast(el.getAttribute('data-flash'), el.getAttribute('data-flash-type') || 'info');
            el.remove();
        });
    });

    /* ---------------- 表单：提交确认 + 轻量校验 ---------------- */
    document.addEventListener('submit', function (e) {
        var form = e.target;
        var confirmText = form.getAttribute('data-confirm');
        if (confirmText && !window.confirm(confirmText)) {
            e.preventDefault();
            return;
        }
        if (form.hasAttribute('data-validate')) {
            var invalid = null;
            form.querySelectorAll('[required]').forEach(function (field) {
                if (invalid) return;
                if (!String(field.value || '').trim()) {
                    invalid = field;
                }
            });
            if (invalid) {
                e.preventDefault();
                invalid.classList.add('input-error');
                invalid.focus();
                window.toast('请填写完整后再提交', 'error');
                return;
            }
            var pwd = form.querySelector('[data-password]');
            var confirmPwd = form.querySelector('[data-password-confirm]');
            if (pwd && confirmPwd && pwd.value !== confirmPwd.value) {
                e.preventDefault();
                window.toast('两次输入的密码不一致', 'error');
                confirmPwd.focus();
                return;
            }
        }
        var btn = form.querySelector('button[type="submit"]');
        if (btn && !btn.hasAttribute('data-no-loading')) {
            btn.disabled = true;
            var origin = btn.innerHTML;
            btn.innerHTML = '处理中…';
            setTimeout(function () { btn.disabled = false; btn.innerHTML = origin; }, 6000);
        }
    });

    document.addEventListener('input', function (e) {
        if (e.target.classList.contains('input-error')) {
            e.target.classList.remove('input-error');
        }
    });

    /* ---------------- 评分控件 ---------------- */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-star]');
        if (!btn) return;
        var group = btn.parentElement;
        var value = parseInt(btn.getAttribute('data-star'), 10);
        group.querySelectorAll('[data-star]').forEach(function (s) {
            s.classList.toggle('on', parseInt(s.getAttribute('data-star'), 10) <= value);
        });
        var hidden = document.querySelector('#scoreValue');
        if (hidden) hidden.value = value;
    });

    /* ---------------- 数量加减 ---------------- */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-step]');
        if (!btn) return;
        var input = document.querySelector(btn.getAttribute('data-target'));
        if (!input) return;
        var step = parseInt(btn.getAttribute('data-step'), 10);
        var next = (parseInt(input.value, 10) || 1) + step;
        var min = parseInt(input.getAttribute('min') || '1', 10);
        var max = parseInt(input.getAttribute('max') || '10', 10);
        input.value = Math.min(max, Math.max(min, next));
        var priceEl = document.querySelector('[data-unit-price]');
        var totalEl = document.querySelector('[data-total]');
        if (priceEl && totalEl) {
            var total = parseFloat(priceEl.getAttribute('data-unit-price')) * input.value;
            totalEl.textContent = '¥' + total.toFixed(2);
        }
    });

    /* ---------------- 复制 ---------------- */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-copy]');
        if (!btn) return;
        var text = btn.getAttribute('data-copy');
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(function () {
                window.toast('已复制：' + text, 'success');
            });
        }
    });
})();
