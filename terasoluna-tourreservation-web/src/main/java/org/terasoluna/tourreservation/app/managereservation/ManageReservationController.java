/*
 * Copyright(c) 2013 NTT DATA Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.terasoluna.tourreservation.app.managereservation;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.terasoluna.gfw.common.exception.BusinessException;
import org.terasoluna.gfw.web.token.transaction.TransactionTokenCheck;
import org.terasoluna.gfw.web.token.transaction.TransactionTokenType;
import org.terasoluna.tourreservation.domain.model.Reserve;
import org.terasoluna.tourreservation.domain.service.reserve.ReservationUpdateInput;
import org.terasoluna.tourreservation.domain.service.reserve.ReservationUpdateOutput;
import org.terasoluna.tourreservation.domain.service.reserve.ReserveService;
import org.terasoluna.tourreservation.domain.service.userdetails.ReservationUserDetails;

import com.github.dozermapper.core.Mapper;

@Controller
@RequestMapping(value = "reservations")
@TransactionTokenCheck(value = "reservations")
public class ManageReservationController {

    @Inject
    ManageReservationHelper manageReservationHelper;

    @Inject
    ReserveService reserveService;

    @Inject
    Mapper beanMapper;

    @Value("${reservation.reportPdfName}")
    String reservationReportPdfName;

    /**
     * pre-initialization of form backed bean
     * @return
     */
    @ModelAttribute
    public ManageReservationForm setUpManageReservationForm() {
        ManageReservationForm form = new ManageReservationForm();
        return form;
    }

    /**
     * Shows the list of reservations for a particular user
     * @param model
     * @return
     */
    @GetMapping(value = "me")
    public String list(
            @AuthenticationPrincipal ReservationUserDetails userDetails,
            Model model) {
        List<ReserveRowOutput> rows = manageReservationHelper.list(userDetails);

        model.addAttribute("rows", rows);
        return "managereservation/list";
    }

    /**
     * Shows the detailed information of a reservation
     * @param reserveNo
     * @param model
     * @return
     */
    @GetMapping(value = "{reserveNo}")
    public String detailForm(@PathVariable("reserveNo") String reserveNo,
            Model model) {
        ReservationDetailOutput output = manageReservationHelper.findDetail(
                reserveNo);
        model.addAttribute("output", output);
        return "managereservation/detailForm";
    }

    /**
     * Shows the edit screen for user to make changes to the reservation. Uses reserveNo from the path to fetch the reservation
     * info
     * @param reserveNo
     * @param form
     * @param model
     * @return
     */
    @GetMapping(value = "{reserveNo}/update", params = "form")
    public String updateForm(@PathVariable("reserveNo") String reserveNo,
            ManageReservationForm form, Model model) {

        Reserve reserve = reserveService.findOne(reserveNo);

        // Map Model to form
        // This is needed to copy the current values of the reservation into the form
        beanMapper.map(reserve, form);

        model.addAttribute(reserve);
        return "managereservation/updateForm";
    }

    /**
     * Goes back to the edit screen for user to make changes to the reservation.
     * @param reserveNo
     * @param model
     * @return
     */
    @PostMapping(value = "{reserveNo}/update", params = "redo")
    public String updateRedo(@PathVariable("reserveNo") String reserveNo,
            ManageReservationForm form, Model model) {
        Reserve reserve = reserveService.findOne(reserveNo);
        model.addAttribute(reserve);
        return "managereservation/updateForm";
    }

    /**
     * Shows the confirmation page after user changes edits the reservation info on the edit reservation page
     * @param model
     * @return
     */
    @TransactionTokenCheck(value = "update", type = TransactionTokenType.BEGIN)
    @PostMapping(value = "{reserveNo}/update", params = "confirm")
    public String updateConfirm(@PathVariable("reserveNo") String reserveNo,
            @Validated ManageReservationForm form, BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            return updateRedo(reserveNo, form, model);
        }

        ReservationDetailOutput output = manageReservationHelper.findDetail(
                reserveNo, form);
        model.addAttribute("output", output);
        return "managereservation/updateConfirm";
    }

    /**
     * Updates the reservation after user changes edits the reservation info on the edit reservation page
     * @param form
     * @return
     */
    @TransactionTokenCheck(value = "update", type = TransactionTokenType.IN)
    @PostMapping(value = "{reserveNo}/update")
    public String update(@PathVariable("reserveNo") String reserveNo,
            @Validated ManageReservationForm form, BindingResult result,
            Model model, RedirectAttributes redirectAttr) {
        if (result.hasErrors()) {
            return updateRedo(reserveNo, form, model);
        }

        ReservationUpdateInput input = beanMapper.map(form,
                ReservationUpdateInput.class);
        input.setReserveNo(reserveNo);

        ReservationUpdateOutput output = reserveService.update(input);
        redirectAttr.addFlashAttribute("output", output);
        return "redirect:/reservations/{reserveNo}/update?complete";
    }

    /**
     * redirects to the update completion page
     * @return
     */
    @GetMapping(value = "{reserveNo}/update", params = "complete")
    public String updateComplete() {
        return "managereservation/updateComplete";
    }

    @PostMapping(value = "{reserveNo}/update", params = "backTolist")
    public String updateBackList() {
        return "redirect:/reservations/me";
    }

    @TransactionTokenCheck(value = "cancel", type = TransactionTokenType.BEGIN)
    @GetMapping(value = "{reserveNo}/cancel")
    public String cancelConfirm(@PathVariable("reserveNo") String reserveNo,
            Model model) {
        ReservationDetailOutput output = manageReservationHelper.findDetail(
                reserveNo);
        model.addAttribute("output", output);
        return "managereservation/cancelConfirm";
    }

    @TransactionTokenCheck(value = "cancel", type = TransactionTokenType.IN)
    @PostMapping(value = "{reserveNo}/cancel")
    public String cancel(@PathVariable("reserveNo") String reserveNo,
            Model model, RedirectAttributes redirectAttr) {
        try {
            reserveService.cancel(reserveNo);
        } catch (BusinessException e) {
            model.addAttribute(e.getResultMessages());
            return cancelConfirm(reserveNo, model);
        }
        return "redirect:/reservations/{reserveNo}/cancel?complete";
    }

    /**
     * redirects to the update completion page
     * @return
     */
    @GetMapping(value = "{reserveNo}/cancel", params = "complete")
    public String cancelComplete(@PathVariable("reserveNo") String reserveNo,
            Model model) {
        model.addAttribute("reserveNo", reserveNo);
        return "managereservation/cancelComplete";
    }

    @PostMapping(value = "{reserveNo}/cancel", params = "backTolist")
    public String backList() {
        return "redirect:/reservations/me";
    }

    @GetMapping(value = "{reserveNo}/pdf")
    public String downloadPDF(@PathVariable("reserveNo") String reserveNo,
            Model model, Locale locale) {
        DownloadPDFOutput downloadPDFOutput = manageReservationHelper.createPDF(
                reserveNo, locale);
        model.addAttribute("downloadPDFOutput", downloadPDFOutput);
        model.addAttribute("downloadPDFName", reservationReportPdfName);
        return "reservationReportPdfStamperView";
    }

}
