package com.hrm.service;

import com.hrm.dto.request.NewCreateCompanyManagerRequestDto;
import com.hrm.dto.request.UpdateCompanyManagerRequestDto;
import com.hrm.dto.response.CompanyManagerDetailResponseDto;
import com.hrm.exception.ErrorType;
import com.hrm.exception.CompanyManagerServiceException;
import com.hrm.mapper.ICompanyManagerMapper;
import com.hrm.rabbitmq.producer.RegisterCompanyManagerProducer;
import com.hrm.repository.ICompanyManagerRepository;
import com.hrm.repository.entity.CompanyManager;
import com.hrm.repository.enums.EStatus;
import com.hrm.utility.ServiceManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyManagerService extends ServiceManager<CompanyManager, String> {
    private final ICompanyManagerRepository companyManagerRepository;
    private final RegisterCompanyManagerProducer registerCompanyManagerProducer;

    public CompanyManagerService(ICompanyManagerRepository companyManagerRepository, RegisterCompanyManagerProducer registerCompanyManagerProducer) {
        super(companyManagerRepository);
        this.companyManagerRepository = companyManagerRepository;
        this.registerCompanyManagerProducer = registerCompanyManagerProducer;
    }

    public Boolean createCompanyManager(NewCreateCompanyManagerRequestDto dto) {
        if (companyManagerRepository.findOptionalByEmail(dto.getEmail()).isPresent())
            throw new CompanyManagerServiceException(ErrorType.EMAIL_DUPLICATE);
        CompanyManager companyManager = ICompanyManagerMapper.INSTANCE.toCompanyManager(dto);
        companyManager.setBirthDate(companyManager.getBirthDate().plusDays(1));
        save(companyManager);
        registerCompanyManagerProducer.sendNewCompanyManager(ICompanyManagerMapper.INSTANCE.toModel(companyManager));
        return true;
    }

    public Boolean updateCompanyManager(UpdateCompanyManagerRequestDto dto) {
        Optional<CompanyManager> companyManager = companyManagerRepository.findById(dto.getId());
        if (companyManager.isEmpty()) {
            throw new CompanyManagerServiceException(ErrorType.ID_NOT_FOUND);
        }
        companyManager.get().setImage(dto.getImage());
        companyManager.get().setEmail(dto.getEmail());
        companyManager.get().setAddress(dto.getAddress());
        companyManager.get().setPhoneNumber(dto.getPhoneNumber());
        update(companyManager.get());
        return true;
    }

    public Boolean delete(String id) {
        Optional<CompanyManager> companyManager = findById(id);
        if (companyManager.isEmpty())
            throw new CompanyManagerServiceException(ErrorType.ID_NOT_FOUND);
        companyManager.get().setStatus(EStatus.DELETED);
        update(companyManager.get());
        return true;
    }

    public List<CompanyManagerDetailResponseDto> findAllByDetail() {
        List<CompanyManager> companyManagerList = findAll();
        List<CompanyManagerDetailResponseDto> companyManagerDetailResponseDtoList = new ArrayList<>();
        companyManagerList.forEach(x->{
            companyManagerDetailResponseDtoList.add(ICompanyManagerMapper.INSTANCE.toCompanyManagerDetailResponseDto(x));
        });
        return companyManagerDetailResponseDtoList;
    }
}
