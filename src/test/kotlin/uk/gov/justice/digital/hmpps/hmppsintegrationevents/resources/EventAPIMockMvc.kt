package uk.gov.justice.digital.hmpps.hmppsintegrationevents.resources

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

@AutoConfigureMockMvc
class EventAPIMockMvc(
  @Autowired var mockMvc: MockMvc,
) {
  fun performAuthorised(path: String): MvcResult {
    val subjectDistinguishedName = "MockService1c"
    return mockMvc.perform(MockMvcRequestBuilders.get(path).header("subject-distinguished-name", subjectDistinguishedName)).andReturn()
  }

  fun performAuthorisedWithCN(
    path: String,
    cn: String,
  ): MvcResult {
    val subjectDistinguishedName = "C=GB,ST=London,L=London,O=Home Office,CN=$cn"
    return mockMvc.perform(MockMvcRequestBuilders.get(path).header("subject-distinguished-name", subjectDistinguishedName)).andReturn()
  }

  fun performUnAuthorised(path: String): MvcResult {
    return mockMvc.perform(MockMvcRequestBuilders.get(path)).andReturn()
  }
}
